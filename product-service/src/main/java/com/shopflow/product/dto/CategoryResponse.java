package com.shopflow.product.dto;

import com.shopflow.product.entity.Category;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(

        UUID id,
        UUID parentId,
        String name,
        String slug,
        List<CategoryResponse> children

) {
    // Flat mapping — no children loaded.
    // Used in dropdowns, filter lists, and product form selectors.
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getName(),
                category.getSlug(),
                List.of()
        );
    }

    // Tree mapping — children are included one level deep.
    // Used in navigation menus and category browse pages.
    public static CategoryResponse withChildren(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getName(),
                category.getSlug(),
                category.getChildren().stream()
                        .map(CategoryResponse::from)
                        .toList()
        );
    }
}