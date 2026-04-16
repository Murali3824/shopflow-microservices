package com.shopflow.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Full MinIO URL e.g. http://localhost:9000/shopflow-products/abc.jpg
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    // Only one image per product should have isPrimary = true
    // Enforced at service layer, not DB constraint (simpler for updates)
    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;
}