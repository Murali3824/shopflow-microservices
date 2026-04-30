package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private UUID orderId;
    private UUID userId;
    private String userEmail;
    private String fullName;
    private BigDecimal amount;
    private String gateway;
    private String gatewayPaymentId;
}