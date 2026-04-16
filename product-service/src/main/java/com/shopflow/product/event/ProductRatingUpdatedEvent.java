package com.shopflow.product.event;

import java.util.UUID;

public record ProductRatingUpdatedEvent(
        UUID productId,
        Double averageRating,
        Long totalReviews
) {}