package com.shopflow.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public record ProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        String description,

        @Size(max = 100, message = "Brand name must not exceed 100 characters")
        String brand,

        @NotNull(message = "Category ID is required")
        UUID categoryId,

        @NotNull(message = "At least one SKU is required")
        @Size(min = 1, message = "At least one SKU is required")
        @Valid
        List<ProductSkuRequest> skus

) {}