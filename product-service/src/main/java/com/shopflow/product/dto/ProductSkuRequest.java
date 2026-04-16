package com.shopflow.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductSkuRequest(

        @NotBlank(message = "SKU code is required")
        @Size(max = 100, message = "SKU code must not exceed 100 characters")
        String skuCode,

        @Size(max = 255, message = "Variant name must not exceed 255 characters")
        String variantName,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Price format invalid")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQty,

        @Min(value = 0, message = "Low stock threshold cannot be negative")
        Integer lowStockThreshold

) {}