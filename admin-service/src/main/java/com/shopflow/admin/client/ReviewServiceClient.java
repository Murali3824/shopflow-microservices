package com.shopflow.admin.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "review-service")
public interface ReviewServiceClient {

    @DeleteMapping("/api/reviews/internal/{reviewId}")
    void deleteReview(@PathVariable("reviewId") UUID reviewId);
}