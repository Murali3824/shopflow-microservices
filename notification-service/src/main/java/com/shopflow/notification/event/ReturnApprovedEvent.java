package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnApprovedEvent {
    private UUID returnRequestId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID userId;
    private String userEmail;    // will be null until order-service enriches it
    private String fullName;     // will be null until order-service enriches it
    private BigDecimal amount;   // FIXED: was "refundAmount"
    private String note;
    private LocalDateTime approvedAt;
}