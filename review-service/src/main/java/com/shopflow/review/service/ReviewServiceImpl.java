package com.shopflow.review.service;

import com.shopflow.review.client.OrderServiceClient;
import com.shopflow.review.dto.ReviewRequest;
import com.shopflow.review.dto.ReviewResponse;
import com.shopflow.review.entity.Review;
import com.shopflow.review.event.ProductRatingUpdatedEvent;
import com.shopflow.review.exception.*;
import com.shopflow.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final String TOPIC_PRODUCT_RATING_UPDATED = "product.rating.updated";

    private final ReviewRepository reviewRepository;
    private final OrderServiceClient orderServiceClient;
    private final KafkaTemplate<String, ProductRatingUpdatedEvent> reviewRatingKafkaTemplate;

    // ─────────────────────────────────────────────
    // Submit Review
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse submitReview(UUID userId, ReviewRequest request) {

        // 1. Check duplicate — one review per user per product
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new DuplicateReviewException("You have already reviewed this product");
        }

        // 2. Verify purchase — order must be DELIVERED and belong to this user
        boolean validPurchase = orderServiceClient.verifyPurchase(
                request.getOrderId(),
                userId,
                request.getProductId()
        );

        if (!validPurchase) {
            throw new PurchaseVerificationException(
                    "You can only review products from a delivered order"
            );
        }

        // 3. Save review
        Review review = Review.builder()
                .userId(userId)
                .productId(request.getProductId())
                .orderId(request.getOrderId())
                .rating(request.getRating())
                .title(request.getTitle())
                .body(request.getBody())
                .build();

        Review saved = reviewRepository.save(review);

        // 4. Publish rating update event
        publishRatingEvent(saved.getProductId());

        log.info("Review submitted — reviewId={} userId={} productId={}",
                saved.getId(), userId, saved.getProductId());

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────
    // Get Product Reviews
    // ─────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getProductReviews(UUID productId, int page, int size) {

        return reviewRepository
                .findByProductId(productId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    // ─────────────────────────────────────────────
    // Update Review
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse updateReview(UUID reviewId, UUID userId, ReviewRequest request) {

        Review review = findReviewById(reviewId);

        // Ownership check
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("You can only update your own review");
        }

        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());

        Review updated = reviewRepository.save(review);

        // Publish rating update event
        publishRatingEvent(updated.getProductId());

        log.info("Review updated — reviewId={} userId={}", reviewId, userId);

        return toResponse(updated);
    }

    // ─────────────────────────────────────────────
    // Delete Review
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReview(UUID reviewId, UUID userId) {

        Review review = findReviewById(reviewId);

        // Ownership check
        if (!review.getUserId().equals(userId)) {
            throw new UnauthorizedReviewAccessException("You can only delete your own review");
        }

        UUID productId = review.getProductId();

        reviewRepository.delete(review);

        // Publish rating update event after deletion
        publishRatingEvent(productId);

        log.info("Review deleted — reviewId={} userId={}", reviewId, userId);
    }

    // ─────────────────────────────────────────────
    // Internal Delete — admin bypass (no ownership check)
    // ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReviewInternal(UUID reviewId) {

        Review review = findReviewById(reviewId);
        UUID productId = review.getProductId();

        reviewRepository.delete(review);

        publishRatingEvent(productId);

        log.info("Review force-deleted by admin — reviewId={}", reviewId);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    private Review findReviewById(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewNotFoundException(
                        "Review not found with id: " + reviewId
                ));
    }

    private void publishRatingEvent(UUID productId) {

        List<Object[]> results = reviewRepository.getRatingStats(productId);

        Double average = 0.0;
        Long total = 0L;

        if (results != null && !results.isEmpty()) {
            Object[] stats = results.get(0);  // ← get first (and only) row
            average = stats[0] != null ? ((Number) stats[0]).doubleValue() : 0.0;
            total   = stats[1] != null ? ((Number) stats[1]).longValue()   : 0L;
        }

        ProductRatingUpdatedEvent event = ProductRatingUpdatedEvent.builder()
                .productId(productId)
                .averageRating(Math.round(average * 10.0) / 10.0)
                .totalReviews(total)
                .build();

        reviewRatingKafkaTemplate.send(
                TOPIC_PRODUCT_RATING_UPDATED,
                productId.toString(),
                event
        );

        log.info("Rating event published — productId={} avg={} total={}",
                productId, event.getAverageRating(), total);
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .productId(review.getProductId())
                .orderId(review.getOrderId())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .createdAt(review.getCreatedAt())
                .build();
    }
}