package com.shopflow.order.consumer;

import com.shopflow.order.entity.Order;
import com.shopflow.order.entity.OrderStatus;
import com.shopflow.order.exception.OrderNotFoundException;
import com.shopflow.order.repository.OrderRepository;
import com.shopflow.order.repository.OrderStatusHistoryRepository;
import com.shopflow.order.entity.OrderStatusHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderRepository             orderRepository;
    private final OrderStatusHistoryRepository historyRepository;

    // ----------------------------------------------------------------
    // Listens to payment.completed topic.
    // Fired by payment-service after successful payment verification.
    //
    // Payload is a generic Map because payment-service owns the
    // PaymentCompletedEvent class — we do not share event classes
    // across services. Deserializing into a Map avoids a cross-service
    // class dependency. We only need orderId from the payload.
    // ----------------------------------------------------------------
    @Transactional
    @KafkaListener(
            topics = "payment.completed",
            groupId = "order-service-group",
            properties = {
                    "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer"
            }
    )
    public void handlePaymentCompleted(String message) {
        try {
            org.json.JSONObject payload = new org.json.JSONObject(message);
            String orderIdStr = payload.optString("orderId", null);

            if (orderIdStr == null || orderIdStr.isBlank()) {
                log.error("payment.completed event missing orderId. Skipping.");
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            "Order not found for payment confirmation: " + orderId));

            if (!order.getStatus().equals(OrderStatus.PENDING.name())) {
                log.warn("Order {} already in status {}. Skipping.", orderId, order.getStatus());
                return;
            }

            order.setStatus(OrderStatus.CONFIRMED.name());
            orderRepository.save(order);

            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .status(OrderStatus.CONFIRMED.name())
                    .note("Payment confirmed. Order is now being processed.")
                    .changedAt(LocalDateTime.now())
                    .build();

            historyRepository.save(history);
            log.info("Order {} moved to CONFIRMED after payment.", orderId);

        } catch (Exception e) {
            log.error("Failed to process payment.completed event: {}", e.getMessage());
        }
    }
}