package com.shopflow.payment.event;

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
    private String userEmail;
    private String fullName;
    private BigDecimal amount;
    private String note;
    private LocalDateTime approvedAt;
}