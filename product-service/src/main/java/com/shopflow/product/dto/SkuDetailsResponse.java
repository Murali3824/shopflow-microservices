package com.shopflow.product.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record SkuDetailsResponse(
        UUID skuId,
        UUID productId,
        UUID sellerId,
        String productName,
        BigDecimal price,
        Integer stockQty
) {}