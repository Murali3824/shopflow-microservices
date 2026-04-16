package com.shopflow.product.config;

import com.shopflow.product.event.ProductLowStockEvent;
import com.shopflow.product.event.ProductRatingUpdatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // =========================================================
    // COMMON CONFIG
    // =========================================================
    private Map<String, Object> baseConfig() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                ErrorHandlingDeserializer.class);

        config.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
                StringDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
                JsonDeserializer.class);

        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shopflow.*");

        // 🔥 IMPORTANT FIX
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return config;
    }

    // =========================================================
    // RATING EVENT
    // =========================================================
    @Bean
    public ConsumerFactory<String, ProductRatingUpdatedEvent> ratingConsumerFactory() {
        Map<String, Object> config = baseConfig();

        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                ProductRatingUpdatedEvent.class);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductRatingUpdatedEvent>
    ratingKafkaListenerContainerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<String,
                ProductRatingUpdatedEvent>();

        factory.setConsumerFactory(ratingConsumerFactory());
        factory.setConcurrency(3);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    // =========================================================
    // LOW STOCK EVENT
    // =========================================================
    @Bean
    public ConsumerFactory<String, ProductLowStockEvent> lowStockConsumerFactory() {
        Map<String, Object> config = baseConfig();

        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                ProductLowStockEvent.class);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductLowStockEvent>
    lowStockKafkaListenerContainerFactory() {

        var factory = new ConcurrentKafkaListenerContainerFactory<String,
                ProductLowStockEvent>();

        factory.setConsumerFactory(lowStockConsumerFactory());
        factory.setConcurrency(3);

        factory.getContainerProperties()
                .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    // =========================================================
    // ERROR HANDLER
    // =========================================================
    @Bean
    public org.springframework.kafka.listener.DefaultErrorHandler errorHandler() {
        var backoff = new org.springframework.util.backoff.FixedBackOff(1000L, 3L);

        var handler = new org.springframework.kafka.listener.DefaultErrorHandler(backoff);

        handler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonProcessingException.class,
                org.apache.kafka.common.errors.SerializationException.class
        );

        return handler;
    }
}