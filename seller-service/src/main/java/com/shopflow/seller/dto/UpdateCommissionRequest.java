package com.shopflow.seller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCommissionRequest {

    @NotNull(message = "Commission rate is required")
    @Min(value = 0, message = "Commission rate cannot be less than 0")
    @Max(value = 100, message = "Commission rate cannot exceed 100")
    private Double commissionRate;
}