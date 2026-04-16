package com.shopflow.product.repository;

import com.shopflow.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Top-level categories only — parent is null means root node
    List<Category> findByParentIsNull();

    Optional<Category> findBySlug(String slug);

    // Fetches a category with its immediate children in one query.
    // Used for building the category navigation tree one level at a time.
    @Query("""
            SELECT DISTINCT c FROM Category c
            LEFT JOIN FETCH c.children
            WHERE c.id = :id
            """)
    Optional<Category> findByIdWithChildren(
            @org.springframework.data.repository.query.Param("id") UUID id
    );
}