package com.shopflow.order.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record CartResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount,
        String appliedCoupon,
        BigDecimal discountAmount,
        BigDecimal finalAmount
) {}