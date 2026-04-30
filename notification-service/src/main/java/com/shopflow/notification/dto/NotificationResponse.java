package com.shopflow.notification.dto;

import com.shopflow.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private String eventType;
    private Notification.NotificationChannel channel;
    private String subject;
    private Notification.NotificationStatus status;
    private int retryCount;
    private ZonedDateTime sentAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .eventType(notification.getEventType())
                .channel(notification.getChannel())
                .subject(notification.getSubject())
                .status(notification.getStatus())
                .retryCount(notification.getRetryCount())
                .sentAt(notification.getSentAt())
                .build();
    }
}