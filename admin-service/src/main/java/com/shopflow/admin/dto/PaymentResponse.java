package com.shopflow.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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