package com.shopflow.notification.service;

import com.shopflow.notification.dto.NotificationResponse;
import com.shopflow.notification.entity.Notification;
import com.shopflow.notification.exception.NotificationNotFoundException;
import com.shopflow.notification.exception.UnauthorizedAccessException;
import com.shopflow.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public List<NotificationResponse> getNotificationsForUser(UUID userId) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Override
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() ->
                        new NotificationNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Access denied");
        }

        notification.setRead(true);
        notificationRepository.save(notification);

        return NotificationResponse.from(notification);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications =
                notificationRepository.findByUserIdOrderBySentAtDesc(userId);

        notifications.forEach(notification -> notification.setRead(true));

        notificationRepository.saveAll(notifications);
    }

    @Override
    public Notification saveNotification(Notification notification) {
        return notificationRepository.save(notification);
    }
}