package com.shopflow.notification.consumer;

import com.shopflow.notification.event.OrderDeliveredEvent;
import com.shopflow.notification.event.OrderPlacedEvent;
import com.shopflow.notification.event.OrderShippedEvent;
import com.shopflow.notification.service.EmailService;
import com.shopflow.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final EmailService emailService;
    private final NotificationPersistenceService persistenceService;

    @KafkaListener(
            topics = "order.placed",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderPlacedFactory"
    )
    public void handleOrderPlaced(OrderPlacedEvent event) {

        log.info("Received order.placed event for orderId={}", event.getOrderId());
        log.debug("Event data — userId={}, userEmail={}, fullName={}, totalAmount={}",
                event.getUserId(), event.getUserEmail(),
                event.getFullName(), event.getTotalAmount());

        UUID userId = event.getUserId();
        String subject = "Your order has been placed successfully";

        try {
            emailService.sendOrderConfirmation(event);
            persistenceService.save(userId, "order.placed", subject);
            log.info("Successfully processed order.placed for orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order.placed for orderId={}", event.getOrderId(), e);
            try {
                persistenceService.saveFailed(userId, "order.placed", subject);
            } catch (Exception persistEx) {
                log.error("Failed to persist FAILED notification for orderId={}",
                        event.getOrderId(), persistEx);
            }
        }
    }

    @KafkaListener(
            topics = "order.shipped",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderShippedFactory"
    )
    public void handleOrderShipped(OrderShippedEvent event) {

        log.info("Received order.shipped event for orderId={}", event.getOrderId());
        log.debug("Event data — userId={}, userEmail={}",
                event.getUserId(), event.getUserEmail());

        UUID userId = event.getUserId();
        String subject = "Your order has been shipped";

        try {
            emailService.sendOrderShipped(event);
            persistenceService.save(userId, "order.shipped", subject);
            log.info("Successfully processed order.shipped for orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order.shipped for orderId={}", event.getOrderId(), e);
            try {
                persistenceService.saveFailed(userId, "order.shipped", subject);
            } catch (Exception persistEx) {
                log.error("Failed to persist FAILED notification for orderId={}",
                        event.getOrderId(), persistEx);
            }
        }
    }

    @KafkaListener(
            topics = "order.delivered",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderDeliveredFactory"
    )
    public void handleOrderDelivered(OrderDeliveredEvent event) {

        log.info("Received order.delivered event for orderId={}", event.getOrderId());
        log.debug("Event data — userId={}, userEmail={}",
                event.getUserId(), event.getUserEmail());

        UUID userId = event.getUserId();
        String subject = "Your order has been delivered";

        try {
            emailService.sendOrderDelivered(event);
            persistenceService.save(userId, "order.delivered", subject);
            log.info("Successfully processed order.delivered for orderId={}", event.getOrderId());

        } catch (Exception e) {
            log.error("Failed to process order.delivered for orderId={}", event.getOrderId(), e);
            try {
                persistenceService.saveFailed(userId, "order.delivered", subject);
            } catch (Exception persistEx) {
                log.error("Failed to persist FAILED notification for orderId={}",
                        event.getOrderId(), persistEx);
            }
        }
    }
}