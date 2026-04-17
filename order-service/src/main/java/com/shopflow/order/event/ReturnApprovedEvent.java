package com.shopflow.order.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
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