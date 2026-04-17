package com.shopflow.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

// - @ManyToOne(fetch = FetchType.LAZY) back to Order — same pattern
//   as OrderItem. The FK (order_id) lives on this side.


@Entity
@Table(name = "order_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK back to the order this history entry belongs to.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // VARCHAR in DB — status value at the moment of this transition.
    @Column(name = "status", nullable = false)
    private String status;

    // Optional context note for this status change.
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Builder.Default
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}