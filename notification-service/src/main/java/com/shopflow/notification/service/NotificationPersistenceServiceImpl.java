package com.shopflow.notification.service;

import com.shopflow.notification.entity.Notification;
import com.shopflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPersistenceServiceImpl implements NotificationPersistenceService {

    private final NotificationRepository notificationRepository;

    @Override
    public void save(UUID userId, String eventType, String subject) {
        Notification notification = Notification.builder()
                .userId(userId)
                .eventType(eventType)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject(subject)
                .status(Notification.NotificationStatus.SENT)
                .retryCount(0)
                .sentAt(ZonedDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public void saveFailed(UUID userId, String eventType, String subject) {
        Notification notification = Notification.builder()
                .userId(userId)
                .eventType(eventType)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject(subject)
                .status(Notification.NotificationStatus.FAILED)
                .retryCount(1)
                .sentAt(ZonedDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public void saveSeller(UUID sellerId, String eventType, String subject) {
        Notification notification = Notification.builder()
                .userId(sellerId) // same column used
                .eventType(eventType)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject(subject)
                .status(Notification.NotificationStatus.SENT)
                .retryCount(0)
                .sentAt(ZonedDateTime.now())
                .build();

        notificationRepository.save(notification);
    }

    @Override
    public void saveSellerFailed(UUID sellerId, String eventType, String subject) {
        Notification notification = Notification.builder()
                .userId(sellerId)
                .eventType(eventType)
                .channel(Notification.NotificationChannel.EMAIL)
                .subject(subject)
                .status(Notification.NotificationStatus.FAILED)
                .retryCount(1)
                .sentAt(ZonedDateTime.now())
                .build();

        notificationRepository.save(notification);
    }
}