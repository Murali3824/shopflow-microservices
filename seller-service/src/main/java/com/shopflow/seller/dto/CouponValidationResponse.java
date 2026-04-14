package com.shopflow.seller.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CouponValidationResponse {

    private String code;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private boolean valid;
}