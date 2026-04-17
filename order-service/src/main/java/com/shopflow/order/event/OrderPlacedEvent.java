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
public class OrderPlacedEvent {

    private UUID orderId;
    private UUID userId;
    private String fullName;
    private String userEmail;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private String addressSnapshot;
    private List<OrderItemData> items;
    private LocalDateTime placedAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemData {
        private UUID productId;
        private UUID skuId;
        private UUID sellerId;
        private String productName;
        private BigDecimal unitPrice;
        private int quantity;
        private BigDecimal subtotal;
    }
}