package com.shopflow.review.event;

import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingUpdatedEvent {

    private UUID productId;
    private Double averageRating;
    private Long totalReviews;
}