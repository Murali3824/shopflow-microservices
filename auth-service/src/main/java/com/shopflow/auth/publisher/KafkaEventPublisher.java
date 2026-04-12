package com.shopflow.auth.publisher;

import com.shopflow.auth.event.OtpRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private static final String OTP_TOPIC = "auth.otp.requested";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOtpRequested(OtpRequestedEvent event) {
        kafkaTemplate.send(OTP_TOPIC, event.getEmail(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish OtpRequestedEvent | email: {} | reason: {}",
                                event.getEmail(), ex.getMessage());
                    } else {
                        log.info("OtpRequestedEvent published | email: {} | otpType: {}",
                                event.getEmail(), event.getOtpType());
                    }
                });
    }
}