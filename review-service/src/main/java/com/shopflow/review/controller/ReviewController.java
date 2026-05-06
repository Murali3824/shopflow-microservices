package com.shopflow.review.controller;

import com.shopflow.review.dto.ReviewRequest;
import com.shopflow.review.dto.ReviewResponse;
import com.shopflow.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ─────────────────────────────────────────────
    // Submit Review
    // ─────────────────────────────────────────────

    @PostMapping
    public ReviewResponse submitReview(@Valid @RequestBody ReviewRequest request) {
        UUID userId = getCurrentUserId();
        return reviewService.submitReview(userId, request);
    }

    // ─────────────────────────────────────────────
    // Get Reviews for Product (Public)
    // ─────────────────────────────────────────────

    @GetMapping("/product/{productId}")
    public Page<ReviewResponse> getProductReviews(
            @PathVariable UUID productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return reviewService.getProductReviews(productId, page, size);
    }

    // ─────────────────────────────────────────────
    // Update Review
    // ─────────────────────────────────────────────

    @PutMapping("/{reviewId}")
    public ReviewResponse updateReview(
            @PathVariable UUID reviewId,
            @Valid @RequestBody ReviewRequest request) {

        UUID userId = getCurrentUserId();
        return reviewService.updateReview(reviewId, userId, request);
    }

    // ─────────────────────────────────────────────
    // Delete Review
    // ─────────────────────────────────────────────

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable UUID reviewId) {
        UUID userId = getCurrentUserId();
        reviewService.deleteReview(reviewId, userId);
    }

    // ─────────────────────────────────────────────
    // Internal — called by admin-service via Feign
    // ─────────────────────────────────────────────

    @DeleteMapping("/internal/{reviewId}")
    public void deleteReviewInternal(@PathVariable UUID reviewId) {
        reviewService.deleteReviewInternal(reviewId);
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private UUID getCurrentUserId() {
        String userId = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        return UUID.fromString(userId);
    }
}