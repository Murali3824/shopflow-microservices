package com.shopflow.notification.repository;

import com.shopflow.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderBySentAtDesc(UUID userId);

    List<Notification> findByUserIdAndStatusOrderBySentAtDesc(
            UUID userId,
            Notification.NotificationStatus status
    );
}