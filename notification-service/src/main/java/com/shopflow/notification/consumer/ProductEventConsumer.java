package com.shopflow.notification.consumer;

import com.shopflow.notification.event.ProductLowStockEvent;
import com.shopflow.notification.service.EmailService;
import com.shopflow.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final EmailService emailService;
    private final NotificationPersistenceService persistenceService;

    @KafkaListener(
            topics = "product.low.stock",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "productLowStockFactory"
    )
    public void handleProductLowStock(ProductLowStockEvent event) {
        log.info("Received product.low.stock event for productId={} skuCode={}",
                event.getProductId(), event.getSkuCode());
        try {
            emailService.sendLowStockAlert(event);
            persistenceService.saveSeller(event.getSellerId(), "product.low.stock",
                    "Low stock alert: " + event.getProductName());
        } catch (Exception e) {
            log.error("Failed to process product.low.stock for productId={}: {}",
                    event.getProductId(), e.getMessage());
            persistenceService.saveSellerFailed(event.getSellerId(), "product.low.stock",
                    "Low stock alert: " + event.getProductName());
        }
    }
}