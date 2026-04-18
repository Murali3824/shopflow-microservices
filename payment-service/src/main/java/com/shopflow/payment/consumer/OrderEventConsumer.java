package com.shopflow.payment.consumer;

import com.shopflow.payment.client.SellerServiceClient;
import com.shopflow.payment.entity.*;
import com.shopflow.payment.repository.PaymentRepository;
import com.shopflow.payment.repository.SellerEarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final PaymentRepository paymentRepository;
    private final SellerEarningRepository sellerEarningRepository;
    private final SellerServiceClient sellerServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    @KafkaListener(topics = "order.placed", groupId = "payment-service-group")
    @Transactional
    public void handleOrderPlaced(String message) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(message);
            String paymentMethod = json.optString("paymentMethod", "");

            if (!Gateway.COD.name().equalsIgnoreCase(paymentMethod)) {
                log.info("Order uses gateway {}. Payment initiated separately via REST.", paymentMethod);
                return;
            }

            String orderId = json.getString("orderId");
            String userId = json.getString("userId");
            double amount = json.getDouble("totalAmount");
            String idempotencyKey = "order-" + orderId;

            if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
                log.info("Payment record already exists for order {}. Skipping.", orderId);
                return;
            }

            Payment payment = Payment.builder()
                    .orderId(UUID.fromString(orderId))
                    .userId(UUID.fromString(userId))
                    .gateway(Gateway.COD.name())
                    .status(PaymentStatus.PENDING.name())
                    .amount(BigDecimal.valueOf(amount))
                    .idempotencyKey(idempotencyKey)
                    .build();

            Payment saved = paymentRepository.save(payment);
            log.info("COD payment record created for order {}", orderId);

            // Auto-confirm COD order — no payment verification needed
            String event = String.format(
                    "{\"orderId\":\"%s\",\"userId\":\"%s\",\"amount\":%s,\"gateway\":\"%s\"}",
                    saved.getOrderId(),
                    saved.getUserId(),
                    saved.getAmount(),
                    saved.getGateway()
            );
            kafkaTemplate.send(TOPIC_PAYMENT_COMPLETED, saved.getOrderId().toString(), event);
            log.info("payment.completed event published for COD order {}", orderId);

        } catch (Exception e) {
            log.error("Failed to process order.placed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "order.delivered", groupId = "payment-service-group")
    @Transactional
    public void handleOrderDelivered(String message) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(message);
            UUID orderId = UUID.fromString(json.getString("orderId"));

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

            if (payment == null) {
                log.warn("No payment found for delivered order {}. Skipping.", orderId);
                return;
            }

            if (Gateway.COD.name().equals(payment.getGateway())) {
                payment.setStatus(PaymentStatus.SUCCESS.name());
                payment.setPaidAt(LocalDateTime.now());
                paymentRepository.save(payment);
                log.info("COD payment marked SUCCESS for order {}", orderId);
            }

            org.json.JSONArray items = json.optJSONArray("sellerEarnings");
            if (items == null) {
                log.warn("No sellerEarnings in order.delivered event for order {}. Skipping.", orderId);
                return;
            }

            for (int i = 0; i < items.length(); i++) {
                org.json.JSONObject item = items.getJSONObject(i);
                UUID orderItemId = UUID.fromString(item.getString("orderItemId"));
                UUID sellerId = UUID.fromString(item.getString("sellerId"));
                BigDecimal subtotal = item.getBigDecimal("grossAmount");

                if (sellerEarningRepository.existsByOrderItemId(orderItemId)) {
                    log.info("Earning already recorded for order item {}. Skipping.", orderItemId);
                    continue;
                }

                BigDecimal commissionRate;
                try {
                    commissionRate = sellerServiceClient
                            .getSellerById(sellerId)
                            .getCommissionRate();
                } catch (Exception e) {
                    log.error("Failed to fetch seller {} commission rate. Skipping item {}.",
                            sellerId, orderItemId);
                    continue;
                }

                BigDecimal commissionAmount = subtotal
                        .multiply(commissionRate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                BigDecimal netEarning = subtotal.subtract(commissionAmount);

                SellerEarning earning = SellerEarning.builder()
                        .sellerId(sellerId)
                        .orderId(orderId)
                        .orderItemId(orderItemId)
                        .grossAmount(subtotal)
                        .commissionRate(commissionRate)
                        .commissionAmount(commissionAmount)
                        .netEarning(netEarning)
                        .build();

                sellerEarningRepository.save(earning);
                log.info("Earning recorded for seller {} on order item {}", sellerId, orderItemId);
            }

        } catch (Exception e) {
            log.error("Failed to process order.delivered event: {}", e.getMessage());
        }
    }
}