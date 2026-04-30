package com.shopflow.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.notification.event.PaymentCompletedEvent;
import com.shopflow.notification.service.EmailService;
import com.shopflow.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final EmailService emailService;
    private final NotificationPersistenceService persistenceService;

    @KafkaListener(
            topics = "payment.completed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "stringFactory"
    )
    public void handlePaymentCompleted(String message) {

        try {
            ObjectMapper mapper = new ObjectMapper();

            PaymentCompletedEvent event =
                    mapper.readValue(message, PaymentCompletedEvent.class);

            emailService.sendPaymentReceipt(event);

            persistenceService.save(
                    event.getUserId(),
                    "payment.completed",
                    "Payment received for your order"
            );

        } catch (Exception e) {
            log.error("Failed to process payment.completed: {}", e.getMessage());
        }
    }
}