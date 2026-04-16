package com.shopflow.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockUpdateRequest(

        @NotNull(message = "SKU ID is required")
        UUID skuId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Operation is required")
        StockOperation operation

) {
    public enum StockOperation {
        INCREMENT,
        DECREMENT
    }
}