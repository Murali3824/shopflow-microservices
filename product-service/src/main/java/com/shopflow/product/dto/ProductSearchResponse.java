package com.shopflow.product.dto;

import com.shopflow.product.document.ProductDocument;

import java.math.BigDecimal;

public record ProductSearchResponse(

        String id,
        String name,
        String brand,
        String categoryId,
        String sellerId,
        BigDecimal avgRating,
        BigDecimal minPrice,
        boolean active

) {
    public static ProductSearchResponse from(ProductDocument document) {
        return new ProductSearchResponse(
                document.id(),
                document.name(),
                document.brand(),
                document.categoryId(),
                document.sellerId(),
                document.avgRating(),
                document.minPrice(),
                document.active()
        );
    }
}