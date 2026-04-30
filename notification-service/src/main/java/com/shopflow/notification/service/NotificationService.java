package com.shopflow.notification.service;

import com.shopflow.notification.dto.NotificationResponse;
import com.shopflow.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getNotificationsForUser(UUID userId);

    NotificationResponse markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    Notification saveNotification(Notification notification);
}