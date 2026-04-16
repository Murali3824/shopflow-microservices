package com.shopflow.product.repository;

import com.shopflow.product.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    // FIXED: explicit JPQL — no productId field on entity
    @Query("SELECT i FROM ProductImage i WHERE i.product.id = :productId")
    List<ProductImage> findByProductId(@Param("productId") UUID productId);

    // FIXED: explicit JPQL — no productId field on entity
    @Query("SELECT i FROM ProductImage i WHERE i.product.id = :productId AND i.primary = true")
    Optional<ProductImage> findByProductIdAndPrimaryTrue(@Param("productId") UUID productId);

    @Modifying
    @Query("""
            UPDATE ProductImage i
            SET i.primary = false
            WHERE i.product.id = :productId
            """)
    void clearPrimaryByProductId(@Param("productId") UUID productId);
}