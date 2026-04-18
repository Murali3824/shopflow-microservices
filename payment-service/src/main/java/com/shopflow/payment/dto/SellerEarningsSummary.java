package com.shopflow.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerEarningsSummary {

    private UUID sellerId;
    private BigDecimal totalNetEarning;
    private BigDecimal totalCommissionPaid;
    private BigDecimal totalGrossAmount;
    private List<SellerEarningResponse> earnings;
}