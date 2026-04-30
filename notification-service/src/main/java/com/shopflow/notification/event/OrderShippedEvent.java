package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippedEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private String fullName;
    private String trackingNumber;
}