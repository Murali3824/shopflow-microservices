package com.shopflow.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private UUID userId;
    private String gateway;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private String status;
    private BigDecimal amount;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}