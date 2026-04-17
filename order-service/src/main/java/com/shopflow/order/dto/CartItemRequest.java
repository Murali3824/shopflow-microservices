package com.shopflow.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CartItemRequest(

        @NotNull(message = "Product ID is required")
        UUID productId,

        @NotNull(message = "SKU ID is required")
        UUID skuId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {}