package com.shopflow.review.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private UUID id;
    private UUID userId;
    private UUID productId;
    private UUID orderId;
    private Integer rating;
    private String title;
    private String body;
    private LocalDateTime createdAt;
}