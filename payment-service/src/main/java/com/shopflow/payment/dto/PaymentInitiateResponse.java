package com.shopflow.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateResponse {

    private UUID paymentId;
    private UUID orderId;
    private String gateway;
    private String gatewayOrderId;
    private String clientSecret;
    private BigDecimal amount;
    private String status;
}