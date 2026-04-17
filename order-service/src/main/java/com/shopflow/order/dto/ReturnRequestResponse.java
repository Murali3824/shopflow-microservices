package com.shopflow.order.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record ReturnRequestResponse(
        UUID id,
        UUID orderId,
        UUID orderItemId,
        String reason,
        String status,
        LocalDateTime requestedAt
) {}