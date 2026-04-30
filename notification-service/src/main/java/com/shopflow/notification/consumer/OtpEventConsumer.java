package com.shopflow.notification.consumer;

import com.shopflow.notification.event.OtpRequestedEvent;
import com.shopflow.notification.service.EmailService;
import com.shopflow.notification.service.NotificationPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OtpEventConsumer {

    private final EmailService emailService;
    private final NotificationPersistenceService persistenceService;

    @KafkaListener(
            topics = "auth.otp.requested",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "otpRequestedFactory"
    )
    public void handleOtpRequested(OtpRequestedEvent event) {

        log.info("Received OTP event for email={}", event.getEmail());

        try {
            emailService.sendOtpEmail(event);

            persistenceService.save(
                    null,
                    "auth.otp.requested",
                    "OTP sent to " + event.getEmail()
            );

        } catch (Exception e) {
            log.error("Failed to process OTP event: {}", e.getMessage());

            persistenceService.saveFailed(
                    null,
                    "auth.otp.requested",
                    "OTP failed for " + event.getEmail()
            );
        }
    }
}
