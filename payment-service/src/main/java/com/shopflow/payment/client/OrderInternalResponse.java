package com.shopflow.payment.client;

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
public class OrderInternalResponse {

    private UUID id;
    private UUID userId;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
}