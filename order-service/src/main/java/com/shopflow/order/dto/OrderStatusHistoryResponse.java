package com.shopflow.order.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record OrderStatusHistoryResponse(
        UUID id,
        String status,
        String note,
        LocalDateTime changedAt
) {}