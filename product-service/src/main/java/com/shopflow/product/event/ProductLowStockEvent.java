package com.shopflow.product.event;

import java.util.UUID;

public record ProductLowStockEvent(

        UUID productId,
        UUID sellerId,
        String sellerEmail,
        String sellerName,
        String productName,
        String skuCode,
        int currentStock,
        int lowStockThreshold

) {}