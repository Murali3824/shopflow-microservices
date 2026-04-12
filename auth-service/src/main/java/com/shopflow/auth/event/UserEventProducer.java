package com.shopflow.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private static final String TOPIC = "user.registered";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC, event.getUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish user.registered event | userId: {} | reason: {}",
                                event.getUserId(), ex.getMessage());
                    } else {
                        log.info("Published user.registered event | userId: {} | email: {} | fullName: {}",
                                event.getUserId(), event.getEmail(), event.getFullName());
                    }
                });
    }
}