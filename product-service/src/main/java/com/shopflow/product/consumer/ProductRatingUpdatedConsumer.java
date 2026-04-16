package com.shopflow.product.consumer;

import com.shopflow.product.event.ProductRatingUpdatedEvent;
import com.shopflow.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRatingUpdatedConsumer {

    private final ProductService productService;

    @KafkaListener(
            topics = "${app.kafka.topics.rating-updated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "ratingKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload ProductRatingUpdatedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Received rating event — productId: {} avgRating: {} partition: {} offset: {}",
                event.productId(), event.averageRating(), partition, offset);

        try {

            if (event == null || event.productId() == null || event.averageRating() == null) {
                log.warn("Invalid rating event received — skipping");
                acknowledgment.acknowledge();
                return;
            }

            productService.updateAvgRating(
                    event.productId(),
                    java.math.BigDecimal.valueOf(event.averageRating())
            );

            acknowledgment.acknowledge();

            log.info("Rating updated successfully — productId: {}", event.productId());

        } catch (Exception ex) {
            log.error("Failed to process rating event — productId: {} error: {}",
                    event.productId(), ex.getMessage(), ex);

            throw ex;
        }
    }
}