package com.shopflow.order.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CartItemResponse(
        UUID productId,
        UUID skuId,
        UUID sellerId,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {}