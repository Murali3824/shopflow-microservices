package com.shopflow.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // JSONB column — stored as raw JSON string in PostgreSQL.
    // Hibernate 6 reads/writes it as JSON automatically via @JdbcTypeCode.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String addressSnapshot;

    @Column(name = "coupon_code")
    private String couponCode;

    // VARCHAR in DB — managed as String here, converted to OrderStatus
    // enum in service layer. Avoids native ENUM conflicts.
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    // VARCHAR in DB — RAZORPAY or STRIPE.
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Builder.Default
    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt = LocalDateTime.now();

    // One order has many items.
    // mappedBy = "order" — the FK lives on OrderItem side.
    // CascadeType.ALL — save/delete order cascades to items.
    // orphanRemoval = true — removing item from list deletes it from DB.
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    // Append-only audit log of status transitions.
    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();
}