package com.shopflow.seller.entity;

import com.shopflow.seller.entity.status.SellerStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    // Reference ID only — no FK to auth-service (cross-service boundary)
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "gst_number", nullable = false, unique = true)
    private String gstNumber;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("10.0");

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "store_description", columnDefinition = "TEXT")
    private String storeDescription;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}