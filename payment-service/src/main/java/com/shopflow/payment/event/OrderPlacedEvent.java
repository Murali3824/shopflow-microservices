package com.shopflow.payment.event;

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
public class OrderPlacedEvent {

    private UUID orderId;
    private UUID userId;
    private BigDecimal totalAmount;
    private String paymentMethod;
}