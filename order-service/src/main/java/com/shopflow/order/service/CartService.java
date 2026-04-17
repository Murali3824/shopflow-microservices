package com.shopflow.order.service;

import com.shopflow.order.dto.CartItemRequest;
import com.shopflow.order.dto.CartResponse;

import java.util.UUID;

public interface CartService {

    CartResponse addItem(UUID userId, CartItemRequest request);

    CartResponse updateItem(UUID userId, UUID skuId, int quantity);

    CartResponse removeItem(UUID userId, UUID skuId);

    CartResponse getCart(UUID userId);

    void clearCart(UUID userId);

    CartResponse applyCoupon(UUID userId, String couponCode);
}