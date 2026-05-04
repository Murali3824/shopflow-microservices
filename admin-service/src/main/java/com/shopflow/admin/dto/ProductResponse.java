package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ProductResponse {
    private UUID id;
    private String name;
    private UUID sellerId;
    private UUID categoryId;
    private String brand;
    private Boolean isActive;
    private BigDecimal avgRating;
}