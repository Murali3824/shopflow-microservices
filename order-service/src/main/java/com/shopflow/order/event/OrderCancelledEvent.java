package com.shopflow.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {

    private UUID orderId;
    private UUID userId;
    private String fullName;
    private String userEmail;
    private String reason;
    private List<StockReleaseItem> items;
    private LocalDateTime cancelledAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockReleaseItem {
        private UUID skuId;
        private int quantity;
    }
}