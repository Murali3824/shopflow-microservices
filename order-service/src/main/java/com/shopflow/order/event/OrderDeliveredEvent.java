package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDeliveredEvent {

    private UUID orderId;
    private UUID userId;
    private String fullName;
    private String userEmail;
    private List<SellerEarningData> sellerEarnings;
    private LocalDateTime deliveredAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SellerEarningData {
        private UUID sellerId;
        private UUID orderItemId;
        private BigDecimal grossAmount;
    }
}