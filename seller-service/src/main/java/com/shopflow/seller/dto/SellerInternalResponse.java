package com.shopflow.seller.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerInternalResponse {
    private UUID id;
    private UUID userId;
    private String businessName;
    private BigDecimal commissionRate;
    private String status;
    private String email;
    private String fullName;
}