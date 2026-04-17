package com.shopflow.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

// Decisions:
// - @ManyToOne(fetch = FetchType.LAZY) — the FK back to Order.
//   LAZY because we don't always need the full Order when we
//   fetch an item. Prevents loading the entire order graph
//   every time an item is touched.
//
// - @JoinColumn(name = "order_id") — tells Hibernate the FK
//   column name in the order_items table. Must match Flyway SQL.

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK to orders table — the owning side of the relationship.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Cross-service reference IDs — no JPA relationships across DBs.
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    // Price snapshot — captured at order placement time.
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Pre-calculated: unit_price × quantity.
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
}