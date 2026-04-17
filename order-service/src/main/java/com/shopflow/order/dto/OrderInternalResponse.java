package com.shopflow.order.dto;

import lombok.*;
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