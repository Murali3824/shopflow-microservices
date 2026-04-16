package com.shopflow.product.repository;

import com.shopflow.product.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface ProductSkuRepository extends JpaRepository<ProductSku, UUID> {

    // FIXED: derived findByProductId won't work — no productId field on entity.
// Use explicit JPQL navigating the association instead.
    @Query("SELECT s FROM ProductSku s WHERE s.product.id = :productId")
    List<ProductSku> findByProductId(@Param("productId") UUID productId);

    Optional<ProductSku> findBySkuCode(String skuCode);

    @Query("""
            SELECT s FROM ProductSku s
            WHERE s.id = :id
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductSku> findByIdForUpdate(@Param("id") UUID id);

    // FIXED: s.product.id instead of s.productId
    // ProductSku has @ManyToOne Product product — JPQL navigates
    // the association. s.productId does not exist as a mapped field.
    @Query("""
            SELECT s FROM ProductSku s
            WHERE s.product.id = :productId
            AND s.stockQty <= s.lowStockThreshold
            """)
    List<ProductSku> findLowStockByProductId(@Param("productId") UUID productId);
}