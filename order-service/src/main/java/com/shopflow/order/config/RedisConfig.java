package com.shopflow.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    // ----------------------------------------------------------------
    // ObjectMapper bean — shared across the entire application.
    // JavaTimeModule registered so LocalDateTime serializes to
    // readable ISO-8601 string instead of throwing an exception.
    // WRITE_DATES_AS_TIMESTAMPS disabled — forces ISO-8601 format
    // e.g. "2024-03-20T10:30:00" instead of [2024,3,20,10,30,0].
    // ----------------------------------------------------------------
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    // ----------------------------------------------------------------
    // StringRedisTemplate bean — used by CartServiceImpl.
    // Stores all Redis values as plain strings (JSON).
    // Spring Boot auto-creates a RedisConnectionFactory from
    // application.yml redis config so we just inject it here.
    // ----------------------------------------------------------------
    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}