package com.shopflow.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_skus")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSku {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    // Human-readable variant label e.g. "Red / XL"
    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty = 0;

    // Kafka low-stock alert fires when stockQty falls to or below this
    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;
}