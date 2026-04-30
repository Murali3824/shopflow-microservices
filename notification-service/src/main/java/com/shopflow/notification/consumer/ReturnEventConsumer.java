package com.shopflow.notification.consumer;

import com.shopflow.notification.event.ReturnApprovedEvent;
import com.shopflow.notification.service.EmailService;
import com.shopflow.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnEventConsumer {

    private final EmailService emailService;
    private final NotificationPersistenceService persistenceService;

    @KafkaListener(
            topics = "return.approved",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "returnApprovedFactory"
    )
    public void handleReturnApproved(ReturnApprovedEvent event) {
        log.info("Received return.approved event for orderId={}", event.getOrderId());

        try {
            emailService.sendRefundInitiated(event);

            persistenceService.save(
                    event.getUserId(),
                    "return.approved",
                    "Your refund has been initiated for order #" + event.getOrderId()
            );

        } catch (Exception e) {
            log.error("Failed to process return.approved for orderId={}: {}",
                    event.getOrderId(), e.getMessage());

            persistenceService.saveFailed(
                    event.getUserId(),
                    "return.approved",
                    "Your refund has been initiated for order #" + event.getOrderId()
            );
        }
    }
}