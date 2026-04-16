package com.shopflow.product.dto;

import com.shopflow.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(

        UUID id,
        UUID sellerId,
        UUID categoryId,
        String categoryName,
        String name,
        String description,
        String brand,
        boolean active,
        BigDecimal avgRating,
        BigDecimal minPrice,
        LocalDateTime createdAt,
        List<ProductSkuResponse> skus,
        List<ProductImageResponse> images

) {
    public static ProductResponse from(Product product) {
        BigDecimal minPrice = product.getSkus().stream()
                .map(ProductSku -> ProductSku.getPrice())
                .min(BigDecimal::compareTo)
                .orElse(null);

        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.isActive(),
                product.getAvgRating(),
                minPrice,
                product.getCreatedAt(),
                product.getSkus().stream()
                        .map(ProductSkuResponse::from)
                        .toList(),
                product.getImages().stream()
                        .map(ProductImageResponse::from)
                        .toList()
        );
    }
}