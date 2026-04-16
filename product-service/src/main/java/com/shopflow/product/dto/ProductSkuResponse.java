package com.shopflow.product.dto;

import com.shopflow.product.entity.ProductSku;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSkuResponse(

        UUID id,
        String skuCode,
        String variantName,
        BigDecimal price,
        int stockQty,
        int lowStockThreshold

) {
    public static ProductSkuResponse from(ProductSku sku) {
        return new ProductSkuResponse(
                sku.getId(),
                sku.getSkuCode(),
                sku.getVariantName(),
                sku.getPrice(),
                sku.getStockQty(),
                sku.getLowStockThreshold()
        );
    }
}