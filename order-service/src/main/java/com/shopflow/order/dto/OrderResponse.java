package com.shopflow.order.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponse(
        UUID id,
        UUID userId,
        String addressSnapshot,
        String couponCode,
        String status,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        String paymentMethod,
        LocalDateTime placedAt,
        List<OrderItemResponse> items,
        List<OrderStatusHistoryResponse> statusHistory
) {}