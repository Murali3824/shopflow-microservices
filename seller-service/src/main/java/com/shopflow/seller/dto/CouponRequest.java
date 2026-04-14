package com.shopflow.seller.dto;

import com.shopflow.seller.entity.type.DiscountType;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record CouponRequest(

        @NotBlank(message = "Coupon code is required")
        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]{3,50}$",
                message = "Coupon code must be uppercase alphanumeric with optional - or _")
        String code,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
        @Digits(integer = 8, fraction = 2, message = "Invalid discount value format")
        BigDecimal discountValue,

        LocalDateTime validUntil,

        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit
) {}