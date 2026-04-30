package com.shopflow.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLowStockEvent {
    private UUID productId;
    private UUID sellerId;
    private String sellerEmail;
    private String sellerName;
    private String productName;
    private String skuCode;
    private int currentStock;
    private int lowStockThreshold;
}