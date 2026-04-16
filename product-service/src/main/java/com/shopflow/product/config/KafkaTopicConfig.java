package com.shopflow.product.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.low-stock}")
    private String lowStockTopic;

    @Value("${app.kafka.topics.rating-updated}")
    private String ratingUpdatedTopic;

    @Bean
    public NewTopic lowStockTopic() {
        return TopicBuilder
                .name(lowStockTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ratingUpdatedTopic() {
        return TopicBuilder
                .name(ratingUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}