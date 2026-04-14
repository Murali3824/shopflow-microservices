package com.shopflow.seller.dto;

import com.shopflow.seller.entity.type.DiscountType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CouponResponse(

        UUID id,
        UUID sellerId,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime validUntil,
        Integer usageLimit,
        Integer timesUsed,
        Boolean isActive
) {}