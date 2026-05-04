package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RevenueReportResponse {
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal totalCommission;
    private BigDecimal platformRevenue;
    private String period;
}