package com.shopflow.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.client.ProductServiceClient;
import com.shopflow.order.client.SellerServiceClient;
import com.shopflow.order.client.UserServiceClient;
import com.shopflow.order.config.KafkaTopicConfig;
import com.shopflow.order.dto.*;
import com.shopflow.order.entity.*;
import com.shopflow.order.event.*;
import com.shopflow.order.exception.*;
import com.shopflow.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository            orderRepository;
    private final OrderItemRepository        orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ReturnRequestRepository    returnRequestRepository;
    private final CartService                cartService;
    private final ProductServiceClient       productClient;
    private final UserServiceClient          userClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper               objectMapper;
    private final SellerServiceClient sellerClient;

    // ----------------------------------------------------------------
    // PLACE ORDER
    // 1. Validate cart is not empty
    // 2. Validate payment method
    // 3. Fetch address from user-service → build addressSnapshot JSON
    // 4. Build Order + OrderItems from cart
    // 5. Decrement stock via product-service for each SKU
    // 6. Save order to DB
    // 7. Record PENDING status history entry
    // 8. Clear cart from Redis
    // 9. Fire order.placed Kafka event
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public OrderResponse placeOrder(UUID userId, OrderRequest request) {

        // Step 1 — validate payment method
        validatePaymentMethod(request.paymentMethod());

        // Step 2 — fetch cart
        CartResponse cart = cartService.getCart(userId);
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new CartEmptyException("Cart is empty. Add items before placing an order.");
        }

        // Step 3 — fetch address from user-service and snapshot it
        UserServiceClient.UserAddressResponse address = userClient.getUserAddress(
                userId,
                userId.toString(),
                "USER"
        );
        String addressSnapshot = serializeAddress(address);

        // Step 4 — calculate totals
        BigDecimal discountAmount = BigDecimal.ZERO;
        String appliedCoupon = cart.appliedCoupon();

        if (appliedCoupon != null && !appliedCoupon.isBlank()) {
            try {
                SellerServiceClient.CouponValidationResponse coupon =
                        sellerClient.validateCoupon(appliedCoupon, cart.totalAmount());
                discountAmount = coupon.getDiscountAmount();
            } catch (Exception e) {
                log.warn("Coupon validation failed for code: {}. Proceeding without discount.",
                        appliedCoupon);
                appliedCoupon = null;
            }
        }

        BigDecimal finalAmount = cart.totalAmount().subtract(discountAmount);

        // Step 5 — build Order entity
        Order order = Order.builder()
                .userId(userId)
                .addressSnapshot(addressSnapshot)
                .couponCode(appliedCoupon)
                .status(OrderStatus.PENDING.name())
                .totalAmount(finalAmount)
                .discountAmount(discountAmount)
                .paymentMethod(request.paymentMethod())
                .placedAt(LocalDateTime.now())
                .build();

        // Step 6 — build OrderItems from cart items
        List<OrderItem> orderItems = cart.items().stream()
                .map(cartItem -> OrderItem.builder()
                        .order(order)
                        .productId(cartItem.productId())
                        .skuId(cartItem.skuId())
                        .sellerId(cartItem.sellerId())
                        .productName(cartItem.productName())
                        .unitPrice(cartItem.unitPrice())
                        .quantity(cartItem.quantity())
                        .subtotal(cartItem.subtotal())
                        .build())
                .collect(Collectors.toList());

        order.getItems().addAll(orderItems);

        // Step 7 — decrement stock for each SKU via product-service
        for (CartItemResponse cartItem : cart.items()) {
            productClient.updateStock(
                    ProductServiceClient.StockUpdateRequest.builder()
                            .skuId(cartItem.skuId())
                            .quantity(cartItem.quantity())
                            .operation("DECREMENT")
                            .build()
            );
        }

        // Step 8 — save order (cascades to items)
        Order savedOrder = orderRepository.save(order);

        // Step 9 — record PENDING status history
        recordHistory(savedOrder, OrderStatus.PENDING.name(), "Order placed successfully");

        // Step 10 — clear cart
        cartService.clearCart(userId);

        UserServiceClient.UserProfileResponse userProfile =
                userClient.getUserProfile(userId, userId.toString(), "USER");

        String email = userProfile.getEmail();
        String fullName = userProfile.getFullName();

        // Step 11 — fire order.placed Kafka event
        List<OrderPlacedEvent.OrderItemData> eventItems = savedOrder.getItems()
                .stream()
                .map(item -> OrderPlacedEvent.OrderItemData.builder()
                        .productId(item.getProductId())
                        .skuId(item.getSkuId())
                        .sellerId(item.getSellerId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(userId)
                .userEmail(email)  // enriched here from user-service
                .fullName(fullName) // enriched here from user-service
                .paymentMethod(savedOrder.getPaymentMethod())
                .totalAmount(savedOrder.getTotalAmount())
                .addressSnapshot(savedOrder.getAddressSnapshot())
                .items(eventItems)
                .placedAt(savedOrder.getPlacedAt())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.ORDER_PLACED,
                savedOrder.getId().toString(), event);

        log.info("Order placed successfully. orderId={}", savedOrder.getId());

        return mapToOrderResponse(savedOrder);
    }

    // ----------------------------------------------------------------
    // GET ORDER
    // Scoped to userId — user cannot fetch another user's order.
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));
        return mapToOrderResponse(order);
    }

    // ----------------------------------------------------------------
    // LIST ORDERS
    // Paginated list of orders for the logged-in user.
    // ----------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(UUID userId, int page, int size) {
        return orderRepository.findByUserIdOrderByPlacedAtDesc(userId)
                .stream()
                .skip((long) page * size)
                .limit(size)
                .map(this::mapToOrderResponse)
                .toList();
    }

    // ----------------------------------------------------------------
    // CANCEL ORDER
    // Only allowed before SHIPPED status.
    // Releases stock back via product-service.
    // Fires order.cancelled Kafka event.
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getStatus().equals(OrderStatus.PENDING.name()) &&
                !order.getStatus().equals(OrderStatus.CONFIRMED.name())) {
            throw new OrderCancellationNotAllowedException(
                    "Order cannot be cancelled. Current status: "
                            + order.getStatus()
                            + ". Only PENDING or CONFIRMED orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        orderRepository.save(order);
        recordHistory(order, OrderStatus.CANCELLED.name(), "Cancelled by user");

        // Release stock back for each item
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productClient.updateStock(
                    ProductServiceClient.StockUpdateRequest.builder()
                            .skuId(item.getSkuId())
                            .quantity(item.getQuantity())
                            .operation("INCREMENT")
                            .build()
            );
        }

        // Fire order.cancelled Kafka event
        List<OrderCancelledEvent.StockReleaseItem> releaseItems = items.stream()
                .map(item -> OrderCancelledEvent.StockReleaseItem.builder()
                        .skuId(item.getSkuId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        UserServiceClient.UserProfileResponse userProfile =
                userClient.getUserProfile(userId,
                        userId.toString(), "USER");

        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .orderId(orderId)
                .userId(userId)
                .userEmail(userProfile.getEmail())
                .fullName(userProfile.getFullName())
                .reason("Cancelled by user")
                .items(releaseItems)
                .cancelledAt(LocalDateTime.now())
                .build();
        kafkaTemplate.send(KafkaTopicConfig.ORDER_CANCELLED,
                orderId.toString(), event);

        log.info("Order cancelled. orderId={}", orderId);

        return mapToOrderResponse(order);
    }

    // ----------------------------------------------------------------
    // MARK SHIPPED
    // Called by seller. Moves order CONFIRMED → SHIPPED.
    // Tracking number stored in status history note.
    // Fires order.shipped Kafka event.
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public OrderResponse markShipped(UUID sellerId, UUID orderId,
                                     String trackingNumber) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getStatus().equals(OrderStatus.CONFIRMED.name())) {
            throw new RuntimeException(
                    "Order must be CONFIRMED before it can be marked as shipped.");
        }

        order.setStatus(OrderStatus.SHIPPED.name());
        orderRepository.save(order);
        recordHistory(order, OrderStatus.SHIPPED.name(),
                "Tracking number: " + trackingNumber);

        UserServiceClient.UserProfileResponse userProfile =
                userClient.getUserProfile(order.getUserId(),
                        order.getUserId().toString(), "USER");

        OrderShippedEvent event = OrderShippedEvent.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .userEmail(userProfile.getEmail())
                .fullName(userProfile.getFullName())
                .trackingNumber(trackingNumber)
                .build();

        kafkaTemplate.send(KafkaTopicConfig.ORDER_SHIPPED,
                orderId.toString(), event);

        log.info("Order marked as shipped. orderId={} tracking={}",
                orderId, trackingNumber);

        return mapToOrderResponse(order);
    }

    // ----------------------------------------------------------------
    // MARK DELIVERED
    // Called internally or by admin after delivery confirmation.
    // Fires order.delivered Kafka event.
    // payment-service listens and calculates seller earnings.
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public OrderResponse markDelivered(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getStatus().equals(OrderStatus.SHIPPED.name())) {
            throw new RuntimeException(
                    "Order must be SHIPPED before it can be marked as delivered.");
        }

        order.setStatus(OrderStatus.DELIVERED.name());
        orderRepository.save(order);
        recordHistory(order, OrderStatus.DELIVERED.name(),
                "Order delivered successfully");

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        List<OrderDeliveredEvent.SellerEarningData> earningData = items.stream()
                .map(item -> OrderDeliveredEvent.SellerEarningData.builder()
                        .sellerId(item.getSellerId())
                        .orderItemId(item.getId())
                        .grossAmount(item.getSubtotal())
                        .build())
                .toList();

        UserServiceClient.UserProfileResponse userProfile =
                userClient.getUserProfile(order.getUserId(),
                        order.getUserId().toString(), "USER");

        OrderDeliveredEvent event = OrderDeliveredEvent.builder()
                .orderId(orderId)
                .userId(order.getUserId())
                .userEmail(userProfile.getEmail())
                .fullName(userProfile.getFullName())
                .sellerEarnings(earningData)
                .deliveredAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.ORDER_DELIVERED,
                orderId.toString(), event);

        log.info("Order marked as delivered. orderId={}", orderId);

        return mapToOrderResponse(order);
    }

    // ----------------------------------------------------------------
    // RAISE RETURN
    // Only allowed on DELIVERED orders.
    // One return request per order item enforced.
    // ----------------------------------------------------------------
    @Override
    @Transactional
    public ReturnRequestResponse raiseReturn(UUID userId, UUID orderId,
                                             ReturnRequestRequest request) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        if (!order.getStatus().equals(OrderStatus.DELIVERED.name())) {
            throw new ReturnRequestException(
                    "Returns can only be raised on delivered orders.");
        }

        OrderItem orderItem = orderItemRepository.findById(request.orderItemId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order item not found: " + request.orderItemId()));

        if (returnRequestRepository.existsByOrderItemId(request.orderItemId())) {
            throw new ReturnRequestException(
                    "A return request already exists for this item.");
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .orderItem(orderItem)
                .reason(request.reason())
                .status(ReturnStatus.REQUESTED.name())
                .requestedAt(LocalDateTime.now())
                .build();

        ReturnRequest saved = returnRequestRepository.save(returnRequest);

        log.info("Return request raised. orderId={} itemId={}",
                orderId, request.orderItemId());

        return ReturnRequestResponse.builder()
                .id(saved.getId())
                .orderId(orderId)
                .orderItemId(saved.getOrderItem().getId())
                .reason(saved.getReason())
                .status(saved.getStatus())
                .requestedAt(saved.getRequestedAt())
                .build();
    }


    @Override
    public OrderInternalResponse getOrderInternal(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));

        return OrderInternalResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public boolean verifyPurchase(UUID orderId, UUID userId, UUID productId) {

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 1. Check user
        if (!order.getUserId().equals(userId)) {
            return false;
        }

        // 2. Check status
        if (!order.getStatus().equals("DELIVERED")) {
            return false;
        }

        // 3. Check product exists in order_items table
        return orderItemRepository.existsByOrderIdAndProductId(orderId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReturnRequestResponse> getAllReturnRequests(Pageable pageable) {
        return returnRequestRepository.findAll(pageable)
                .map(returnRequest -> ReturnRequestResponse.builder()
                        .id(returnRequest.getId())
                        .orderId(returnRequest.getOrder().getId())
                        .orderItemId(returnRequest.getOrderItem().getId())
                        .reason(returnRequest.getReason())
                        .status(returnRequest.getStatus())
                        .requestedAt(returnRequest.getRequestedAt())
                        .build());
    }

    @Override
    @Transactional
    public void approveReturn(UUID returnId, String note) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Return request not found: " + returnId));

        if (!returnRequest.getStatus().equals(ReturnStatus.REQUESTED.name())) {
            throw new ReturnRequestException(
                    "Return request is not in REQUESTED status. Current: "
                            + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.APPROVED.name());
        returnRequestRepository.save(returnRequest);

        // ── Increment stock back for returned item ──────────────
        // When return is approved, product SKU stock is released back
        // Same pattern as cancelOrder stock release
        OrderItem orderItem = returnRequest.getOrderItem();
        productClient.updateStock(
                ProductServiceClient.StockUpdateRequest.builder()
                        .skuId(orderItem.getSkuId())
                        .quantity(orderItem.getQuantity())
                        .operation("INCREMENT")
                        .build()
        );

        log.info("Stock incremented for return — skuId: {} quantity: {}",
                orderItem.getSkuId(), orderItem.getQuantity());

        // Fetch user FIRST before any DB changes
        UUID userId = returnRequest.getOrder().getUserId();

        log.info("Fetching user profile for userId: {}", userId);

        UserServiceClient.UserProfileResponse userProfile =
                userClient.getUserProfile(userId, userId.toString(), "USER");

        log.info("User profile fetched — email: {} name: {}",
                userProfile.getEmail(), userProfile.getFullName());

        ReturnApprovedEvent event = ReturnApprovedEvent.builder()
                .returnRequestId(returnId)
                .orderId(returnRequest.getOrder().getId())
                .orderItemId(returnRequest.getOrderItem().getId())
                .userId(userId)
                .userEmail(userProfile.getEmail())
                .fullName(userProfile.getFullName())
                .amount(returnRequest.getOrderItem().getSubtotal())
                .note(note)
                .approvedAt(LocalDateTime.now())
                .build();

        kafkaTemplate.send(KafkaTopicConfig.RETURN_APPROVED,
                returnId.toString(), event);

        log.info("Return approved — returnId: {} orderId: {}",
                returnId, returnRequest.getOrder().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestResponse getReturnById(UUID returnId) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Return request not found: " + returnId));

        return ReturnRequestResponse.builder()
                .id(returnRequest.getId())
                .orderId(returnRequest.getOrder().getId())
                .orderItemId(returnRequest.getOrderItem().getId())
                .reason(returnRequest.getReason())
                .status(returnRequest.getStatus())
                .requestedAt(returnRequest.getRequestedAt())
                .build();
    }

    @Override
    @Transactional
    public void rejectReturn(UUID returnId, String note) {
        ReturnRequest returnRequest = returnRequestRepository.findById(returnId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Return request not found: " + returnId));

        if (!returnRequest.getStatus().equals(ReturnStatus.REQUESTED.name())) {
            throw new ReturnRequestException(
                    "Return request is not in REQUESTED status. Current: "
                            + returnRequest.getStatus());
        }

        returnRequest.setStatus(ReturnStatus.REJECTED.name());
        returnRequestRepository.save(returnRequest);

        log.info("Return rejected — returnId: {} orderId: {}",
                returnId, returnRequest.getOrder().getId());
    }



    // ----------------------------------------------------------------
    // PRIVATE HELPERS
    // ----------------------------------------------------------------

    private void validatePaymentMethod(String paymentMethod) {
        try {
            PaymentMethod.valueOf(paymentMethod);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid payment method: " + paymentMethod +
                            ". Accepted values: RAZORPAY, STRIPE");
        }
    }

    private void recordHistory(Order order, String status, String note) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .note(note)
                .changedAt(LocalDateTime.now())
                .build();
        historyRepository.save(history);
    }

    private String serializeAddress(UserServiceClient.UserAddressResponse address) {
        try {
            return objectMapper.writeValueAsString(address);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize address snapshot", e);
        }
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderStatusHistory> history = historyRepository
                .findByOrderIdOrderByChangedAtAsc(order.getId());

        List<OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .skuId(item.getSkuId())
                        .sellerId(item.getSellerId())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        List<OrderStatusHistoryResponse> historyResponses = history.stream()
                .map(h -> OrderStatusHistoryResponse.builder()
                        .id(h.getId())
                        .status(h.getStatus())
                        .note(h.getNote())
                        .changedAt(h.getChangedAt())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .addressSnapshot(order.getAddressSnapshot())
                .couponCode(order.getCouponCode())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .paymentMethod(order.getPaymentMethod())
                .placedAt(order.getPlacedAt())
                .items(itemResponses)
                .statusHistory(historyResponses)
                .build();
    }
}