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
public class RefundResponse {

    private UUID id;
    private UUID paymentId;
    private UUID returnRequestId;
    private BigDecimal amount;
    private String gatewayRefundId;
    private String status;
    private LocalDateTime createdAt;
}