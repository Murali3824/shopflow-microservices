package com.shopflow.user.event;

import com.shopflow.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "user.registered",
            groupId = "user-service-group"
    )
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received user.registered event | userId: {}", event.getUserId());
        userService.createInitialProfile(event.getUserId(),event.getFullName(),event.getEmail());
    }
}