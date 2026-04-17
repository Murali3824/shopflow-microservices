package com.shopflow.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_PLACED     = "order.placed";
    public static final String ORDER_CANCELLED  = "order.cancelled";
    public static final String ORDER_SHIPPED    = "order.shipped";
    public static final String ORDER_DELIVERED  = "order.delivered";
    public static final String RETURN_APPROVED = "return.approved";

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name(ORDER_PLACED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder.name(ORDER_CANCELLED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderShippedTopic() {
        return TopicBuilder.name(ORDER_SHIPPED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderDeliveredTopic() {
        return TopicBuilder.name(ORDER_DELIVERED)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic returnApprovedTopic() {
        return TopicBuilder.name(RETURN_APPROVED)
                .partitions(1)
                .replicas(1)
                .build();
    }
}