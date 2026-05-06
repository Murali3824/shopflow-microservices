package com.shopflow.review.repository;

import com.shopflow.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductId(UUID productId, Pageable pageable);

    boolean existsByUserIdAndProductId(UUID userId, UUID productId);

    @Query("""
        SELECT AVG(r.rating), COUNT(r)
        FROM Review r
        WHERE r.productId = :productId
    """)
    List<Object[]> getRatingStats(UUID productId);
}