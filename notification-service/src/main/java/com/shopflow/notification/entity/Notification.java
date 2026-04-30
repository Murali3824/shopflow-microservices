package com.shopflow.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "sent_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private ZonedDateTime sentAt;

    public enum NotificationChannel {
        EMAIL
    }

    public enum NotificationStatus {
        SENT, FAILED
    }
}