package com.shopflow.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


// - @ManyToOne(fetch = FetchType.LAZY) on both order and orderItem —
//   lazy load both sides. We rarely need the full Order or OrderItem
//   graph when just listing return requests.
@Entity
@Table(name = "return_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK to the parent order — kept for admin query convenience.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // FK to the specific item being returned.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    // User-provided return reason — required, not nullable.
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // VARCHAR in DB — REQUESTED / APPROVED / REJECTED / REFUNDED.
    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "REQUESTED";

    @Builder.Default
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();
}