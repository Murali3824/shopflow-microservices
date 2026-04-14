package com.shopflow.seller.dto;

import com.shopflow.seller.entity.status.SellerStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record SellerResponse(

        UUID id,
        UUID userId,
        String businessName,
        String gstNumber,
        String phone,
        SellerStatus status,
        BigDecimal commissionRate,
        String storeName,
        String storeDescription,
        String logoUrl,
        LocalDateTime createdAt
) {}