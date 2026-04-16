package com.shopflow.product.dto;

import com.shopflow.product.entity.ProductImage;

import java.util.UUID;

public record ProductImageResponse(

        UUID id,
        String imageUrl,
        boolean primary

) {
    public static ProductImageResponse from(ProductImage image) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.isPrimary()
        );
    }
}