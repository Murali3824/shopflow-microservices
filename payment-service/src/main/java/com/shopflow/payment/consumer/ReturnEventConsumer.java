package com.shopflow.payment.consumer;

import com.shopflow.payment.repository.SellerEarningRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventConsumer {

    private final SellerEarningRepository sellerEarningRepository;

    @KafkaListener(topics = "return.approved", groupId = "payment-service-group")
    @Transactional
    public void handleReturnApproved(String message) {
        try {
            org.json.JSONObject json = new org.json.JSONObject(message);
            UUID orderId = UUID.fromString(json.getString("orderId"));

            // Refund is already processed by triggerRefundInternal before this event fires.
            // Only reverse seller earnings here.
            sellerEarningRepository.deleteByOrderId(orderId);
            log.info("Seller earnings reversed for order {}", orderId);

        } catch (Exception e) {
            log.error("Failed to process return.approved event: {}", e.getMessage());
        }
    }
}