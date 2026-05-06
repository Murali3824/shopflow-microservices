package com.shopflow.review.service;

import com.shopflow.review.dto.ReviewRequest;
import com.shopflow.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

    ReviewResponse submitReview(UUID userId, ReviewRequest request);

    Page<ReviewResponse> getProductReviews(UUID productId, int page, int size);

    ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewRequest request);

    void deleteReview(UUID reviewId, UUID userId);

    void deleteReviewInternal(UUID reviewId);
}