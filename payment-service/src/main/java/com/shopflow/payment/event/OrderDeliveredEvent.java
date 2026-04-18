package com.shopflow.payment.event;

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
public class OrderDeliveredEvent {

    private UUID orderId;
    private UUID sellerId;
    private List<OrderItemDetail> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDetail {
        private UUID orderItemId;
        private UUID sellerId;
        private BigDecimal subtotal;
    }
}