package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class SellerResponse {
    private UUID id;
    private UUID userId;
    private String businessName;
    private String status;
    private BigDecimal commissionRate;
    private String storeName;
}