package com.shopflow.order.controller;

import com.shopflow.order.dto.*;
import com.shopflow.order.exception.UnauthorizedOrderAccessException;
import com.shopflow.order.service.CartService;
import com.shopflow.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final CartService  cartService;
    private final OrderService orderService;

    // ----------------------------------------------------------------
    // CART ENDPOINTS
    // ----------------------------------------------------------------

    // POST /api/cart/items
    // User adds a product SKU to their cart.
    @PostMapping("/cart/items")
    public ResponseEntity<CartResponse> addCartItem(
            @Valid @RequestBody CartItemRequest request) {

        UUID userId = extractUserId();
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/cart
    // User views their current cart with all items and totals.
    @GetMapping("/cart")
    public ResponseEntity<CartResponse> getCart() {
        UUID userId = extractUserId();
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(response);
    }

    // PUT /api/cart/items/{id}
    // {id} here is skuId — update quantity for that SKU in the cart.
    // Passing quantity = 0 removes the item.
    @PutMapping("/cart/items/{id}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable UUID id,
            @RequestParam int quantity) {

        UUID userId = extractUserId();
        CartResponse response = cartService.updateItem(userId, id, quantity);
        return ResponseEntity.ok(response);
    }

    // DELETE /api/cart/items/{id}
    // {id} here is skuId — removes that SKU entirely from cart.
    @DeleteMapping("/cart/items/{id}")
    public ResponseEntity<CartResponse> removeCartItem(
            @PathVariable UUID id) {

        UUID userId = extractUserId();
        CartResponse response = cartService.removeItem(userId, id);
        return ResponseEntity.ok(response);
    }

    // ----------------------------------------------------------------
    // ORDER ENDPOINTS
    // ----------------------------------------------------------------

    // POST /api/orders
    // User places an order from their current cart.
    // Requires addressId and paymentMethod in request body.
    @PostMapping("/orders")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request) {

        UUID userId = extractUserId();
        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/orders
    // User lists all their own orders. Paginated.
    // Default: page 0, size 10.
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        UUID userId = extractUserId();
        List<OrderResponse> response = orderService.listOrders(userId, page, size);
        return ResponseEntity.ok(response);
    }

    // GET /api/orders/{id}
    // User fetches a specific order detail.
    // Service layer enforces userId match — user cannot see
    // another user's order.
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID id) {

        UUID userId = extractUserId();
        OrderResponse response = orderService.getOrder(userId, id);
        return ResponseEntity.ok(response);
    }

    // POST /api/orders/{id}/cancel
    // User cancels their own order.
    // Only allowed if order is PENDING or CONFIRMED.
    // Service layer enforces the status check.
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID id) {

        UUID userId = extractUserId();
        OrderResponse response = orderService.cancelOrder(userId, id);
        return ResponseEntity.ok(response);
    }

    // POST /api/orders/{id}/return
    // User raises a return request for a specific item in the order.
    // Order must be in DELIVERED status.
    // One return request per order item enforced in service layer.
    @PostMapping("/orders/{id}/return")
    public ResponseEntity<ReturnRequestResponse> raiseReturn(
            @PathVariable UUID id,
            @Valid @RequestBody ReturnRequestRequest request) {

        UUID userId = extractUserId();
        ReturnRequestResponse response = orderService.raiseReturn(
                userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/orders/{id}/ship
    // Seller marks an order as shipped with a tracking number.
    // Restricted to SELLER role only.
    // Seller identity extracted from security context.
    // AFTER
    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<OrderResponse> markShipped(
            @PathVariable UUID id,
            @Valid @RequestBody ShipOrderRequest request) {

        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();

        boolean isSeller = auth.getAuthorities().contains(
                new SimpleGrantedAuthority("ROLE_SELLER"));

        if (!isSeller) {
            throw new UnauthorizedOrderAccessException(
                    "Only sellers can mark orders as shipped.");
        }

        UUID sellerId = extractUserId();
        OrderResponse response = orderService.markShipped(
                sellerId, id, request.trackingNumber());
        return ResponseEntity.ok(response);
    }


    // POST /api/orders/{id}/deliver
// Internal — called by admin-service in production.
// Exposed temporarily for testing purposes.
    @PostMapping("/orders/{id}/deliver")
    public ResponseEntity<OrderResponse> markDelivered(
            @PathVariable UUID id) {

        OrderResponse response = orderService.markDelivered(id);
        return ResponseEntity.ok(response);
    }



    // POST /api/cart/coupon
// User applies a coupon code to their cart.
// Coupon stored in Redis — validated at checkout time.
    @PostMapping("/cart/coupon")
    public ResponseEntity<CartResponse> applyCoupon(
            @RequestParam String couponCode) {

        UUID userId = extractUserId();
        CartResponse response = cartService.applyCoupon(userId, couponCode);
        return ResponseEntity.ok(response);
    }


    // In OrderController — add this endpoint
    @GetMapping("/orders/internal/{orderId}")
    public ResponseEntity<OrderInternalResponse> getOrderInternal(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrderInternal(orderId));
    }

    @GetMapping("/orders/internal/{orderId}/verify-purchase")
    public boolean verifyPurchase(
            @PathVariable UUID orderId,
            @RequestParam UUID userId,
            @RequestParam UUID productId) {

        return orderService.verifyPurchase(orderId, userId, productId);
    }

    // ─── Admin Internal Endpoints ──────────────────────────────────────────────
// Called by admin-service via Feign only.
// No JWT required — whitelisted in SecurityConfig.

    @GetMapping("/orders/internal/all")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Internal — get all orders");
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/orders/internal/returns")
    public ResponseEntity<Page<ReturnRequestResponse>> getAllReturnRequests(
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Internal — get all return requests");
        return ResponseEntity.ok(orderService.getAllReturnRequests(pageable));
    }

    @PutMapping("/orders/internal/returns/{returnId}/approve")
    public ResponseEntity<Void> approveReturn(
            @PathVariable UUID returnId,
            @RequestBody ApproveReturnRequest request) {

        log.info("Internal — approve return: {}", returnId);
        orderService.approveReturn(returnId, request.getNote());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/orders/internal/returns/{returnId}/reject")
    public ResponseEntity<Void> rejectReturn(
            @PathVariable UUID returnId,
            @RequestBody ApproveReturnRequest request) {

        log.info("Internal — reject return: {}", returnId);
        orderService.rejectReturn(returnId, request.getNote());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/orders/internal/returns/{returnId}")
    public ResponseEntity<ReturnRequestResponse> getReturnById(
            @PathVariable UUID returnId) {

        log.info("Internal — get return by id: {}", returnId);
        return ResponseEntity.ok(orderService.getReturnById(returnId));
    }


    // ----------------------------------------------------------------
    // PRIVATE HELPER
    // ----------------------------------------------------------------

    // Extracts the userId set by GatewayAuthFilter from the
    // SecurityContext principal. Principal is the X-User-Id header
    // value set during filter chain. Cast to String then parse to UUID.
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext()
                .getAuthentication();
        return UUID.fromString((String) auth.getPrincipal());
    }
}