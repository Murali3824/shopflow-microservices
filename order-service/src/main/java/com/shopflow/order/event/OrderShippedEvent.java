package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippedEvent {

    private UUID orderId;
    private UUID userId;
    private String fullName;
    private String userEmail;
    private String trackingNumber;
    private LocalDateTime shippedAt;
}