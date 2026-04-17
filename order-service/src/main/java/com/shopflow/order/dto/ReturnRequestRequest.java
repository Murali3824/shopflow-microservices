package com.shopflow.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReturnRequestRequest(

        @NotNull(message = "Order item ID is required")
        UUID orderItemId,

        @NotBlank(message = "Reason is required")
        String reason
) {}