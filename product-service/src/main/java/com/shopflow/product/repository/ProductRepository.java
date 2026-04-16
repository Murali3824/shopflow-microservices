package com.shopflow.product.repository;

import com.shopflow.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findBySellerId(UUID sellerId, Pageable pageable);

    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByActive(boolean active, Pageable pageable);

    Page<Product> findBySellerIdAndActive(UUID sellerId, boolean active, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.skus
            WHERE p.id = :id
            """)
    java.util.Optional<Product> findByIdWithSkus(@Param("id") UUID id);
}