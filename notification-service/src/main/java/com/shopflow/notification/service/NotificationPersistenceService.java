package com.shopflow.notification.service;

import java.util.UUID;

public interface NotificationPersistenceService {

    void save(UUID userId, String eventType, String subject);

    void saveFailed(UUID userId, String eventType, String subject);

    void saveSeller(UUID sellerId, String eventType, String subject);

    void saveSellerFailed(UUID sellerId, String eventType, String subject);
}