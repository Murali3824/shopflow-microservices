package com.shopflow.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name("payment.completed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder.name("order.placed")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic orderDeliveredTopic() {
        return TopicBuilder.name("order.delivered")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic returnApprovedTopic() {
        return TopicBuilder.name("return.approved")
                .partitions(1)
                .replicas(1)
                .build();
    }
}