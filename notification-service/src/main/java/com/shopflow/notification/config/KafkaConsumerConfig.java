package com.shopflow.notification.config;

import com.shopflow.notification.event.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    // ============================================================
    // 🔵 COMMON JSON CONSUMER FACTORY (for Order, Product, Return,Otp)
    // ============================================================
    private <T> ConsumerFactory<String, T> jsonConsumerFactory(Class<T> targetType) {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.shopflow.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(targetType, false)
        );
    }

    private <T> ConcurrentKafkaListenerContainerFactory<String, T> jsonFactory(Class<T> targetType) {
        ConcurrentKafkaListenerContainerFactory<String, T> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory(targetType));
        return factory;
    }

    // ============================================================
    // 🟢 STRING CONSUMER FACTORY (for Payment)
    // ============================================================
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> factory =
                new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new StringDeserializer()
                );

        ConcurrentKafkaListenerContainerFactory<String, String> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();

        containerFactory.setConsumerFactory(factory);
        return containerFactory;
    }

    // ============================================================
    // 🔵 JSON EVENT FACTORIES
    // ============================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent>
    orderPlacedFactory() {
        return jsonFactory(OrderPlacedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderShippedEvent>
    orderShippedFactory() {
        return jsonFactory(OrderShippedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderDeliveredEvent>
    orderDeliveredFactory() {
        return jsonFactory(OrderDeliveredEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReturnApprovedEvent>
    returnApprovedFactory() {
        return jsonFactory(ReturnApprovedEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductLowStockEvent>
    productLowStockFactory() {
        return jsonFactory(ProductLowStockEvent.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OtpRequestedEvent>
    otpRequestedFactory() {
        return jsonFactory(OtpRequestedEvent.class);
    }


}