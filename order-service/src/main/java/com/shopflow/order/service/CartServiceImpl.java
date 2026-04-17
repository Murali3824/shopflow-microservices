package com.shopflow.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopflow.order.client.ProductServiceClient;
import com.shopflow.order.dto.CartItemRequest;
import com.shopflow.order.dto.CartItemResponse;
import com.shopflow.order.dto.CartResponse;
import com.shopflow.order.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductServiceClient productClient;

    // Cart key pattern: cart:{userId}
    // Each field in the hash is skuId, value is JSON CartItemResponse
    private static final String CART_KEY_PREFIX = "cart:";
    private static final Duration CART_TTL = Duration.ofDays(7);

    // Coupon key pattern: cart:coupon:{userId}
    private static final String COUPON_KEY_PREFIX = "cart:coupon:";

    // ----------------------------------------------------------------
    // ADD ITEM
    // If skuId already exists in cart, quantity is incremented.
    // If new, a new entry is created.
    // TTL is reset on every modification — 7 days from last activity.
    // ----------------------------------------------------------------
    @Override
    public CartResponse addItem(UUID userId, CartItemRequest request) {
        String cartKey = cartKey(userId);

        // Validate SKU exists in product-service and get real details
        ProductServiceClient.SkuDetailsResponse sku;
        try {
            sku = productClient.getSkuDetails(request.skuId());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Product SKU not found: " + request.skuId());
        }

        // Check stock availability
        if (sku.getStockQty() < request.quantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + sku.getStockQty());
        }

        String existingJson = (String) redisTemplate.opsForHash()
                .get(cartKey, request.skuId().toString());

        CartItemResponse item;

        if (existingJson != null) {
            CartItemResponse existing = deserialize(existingJson);
            int newQuantity = existing.quantity() + request.quantity();

            // Re-check stock for merged quantity
            if (sku.getStockQty() < newQuantity) {
                throw new InsufficientStockException(
                        "Insufficient stock for requested quantity. Available: "
                                + sku.getStockQty());
            }

            BigDecimal newSubtotal = sku.getPrice()
                    .multiply(BigDecimal.valueOf(newQuantity));

            item = CartItemResponse.builder()
                    .productId(existing.productId())
                    .skuId(existing.skuId())
                    .sellerId(existing.sellerId())
                    .productName(sku.getProductName())
                    .unitPrice(sku.getPrice())
                    .quantity(newQuantity)
                    .subtotal(newSubtotal)
                    .build();
        } else {
            BigDecimal subtotal = sku.getPrice()
                    .multiply(BigDecimal.valueOf(request.quantity()));

            item = CartItemResponse.builder()
                    .productId(sku.getProductId())
                    .skuId(sku.getSkuId())
                    .sellerId(sku.getSellerId())
                    .productName(sku.getProductName())
                    .unitPrice(sku.getPrice())
                    .quantity(request.quantity())
                    .subtotal(subtotal)
                    .build();
        }

        redisTemplate.opsForHash().put(cartKey,
                request.skuId().toString(), serialize(item));
        redisTemplate.expire(cartKey, CART_TTL);

        return buildCartResponse(userId);
    }

    // ----------------------------------------------------------------
    // UPDATE ITEM
    // Replaces quantity for the given skuId.
    // If quantity is 0, item is removed entirely.
    // ----------------------------------------------------------------
    @Override
    public CartResponse updateItem(UUID userId, UUID skuId, int quantity) {
        String cartKey = cartKey(userId);

        if (quantity <= 0) {
            redisTemplate.opsForHash().delete(cartKey, skuId.toString());
            redisTemplate.expire(cartKey, CART_TTL);
            return buildCartResponse(userId);
        }

        String existingJson = (String) redisTemplate.opsForHash()
                .get(cartKey, skuId.toString());

        if (existingJson == null) {
            throw new RuntimeException("Item not found in cart: " + skuId);
        }

        // ADD THIS — validate stock before updating quantity
        ProductServiceClient.SkuDetailsResponse sku;
        try {
            sku = productClient.getSkuDetails(skuId);
        } catch (Exception e) {
            throw new RuntimeException("Product SKU not found: " + skuId);
        }

        if (sku.getStockQty() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock. Requested: " + quantity
                            + " Available: " + sku.getStockQty());
        }

        CartItemResponse existing = deserialize(existingJson);
        BigDecimal newSubtotal = sku.getPrice()
                .multiply(BigDecimal.valueOf(quantity));

        CartItemResponse updated = CartItemResponse.builder()
                .productId(existing.productId())
                .skuId(existing.skuId())
                .sellerId(existing.sellerId())
                .productName(existing.productName())
                .unitPrice(existing.unitPrice())
                .quantity(quantity)
                .subtotal(newSubtotal)
                .build();

        redisTemplate.opsForHash().put(cartKey,
                skuId.toString(), serialize(updated));
        redisTemplate.expire(cartKey, CART_TTL);

        return buildCartResponse(userId);
    }

    // ----------------------------------------------------------------
    // REMOVE ITEM
    // Deletes the hash field for the given skuId.
    // ----------------------------------------------------------------
    @Override
    public CartResponse removeItem(UUID userId, UUID skuId) {
        String cartKey = cartKey(userId);
        redisTemplate.opsForHash().delete(cartKey, skuId.toString());
        redisTemplate.expire(cartKey, CART_TTL);
        return buildCartResponse(userId);
    }

    // ----------------------------------------------------------------
    // GET CART
    // Reads all hash fields and builds the full CartResponse.
    // ----------------------------------------------------------------
    @Override
    public CartResponse getCart(UUID userId) {
        return buildCartResponse(userId);
    }

    // ----------------------------------------------------------------
    // CLEAR CART
    // Deletes the entire cart hash and coupon key from Redis.
    // Called after order is successfully placed.
    // ----------------------------------------------------------------
    @Override
    public void clearCart(UUID userId) {
        redisTemplate.delete(cartKey(userId));
        redisTemplate.delete(couponKey(userId));
    }

    // ----------------------------------------------------------------
    // APPLY COUPON
    // Stores the coupon code in a separate Redis key.
    // Actual validation and discount calculation happens at
    // checkout time in OrderService via seller-service Feign call.
    // TTL matches cart TTL — coupon expires with the cart.
    // ----------------------------------------------------------------
    @Override
    public CartResponse applyCoupon(UUID userId, String couponCode) {
        redisTemplate.opsForValue().set(
                couponKey(userId),
                couponCode,
                CART_TTL
        );
        return buildCartResponse(userId);
    }

    // ----------------------------------------------------------------
    // PRIVATE HELPERS
    // ----------------------------------------------------------------

    private String cartKey(UUID userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String couponKey(UUID userId) {
        return COUPON_KEY_PREFIX + userId;
    }

    private String serialize(CartItemResponse item) {
        try {
            return objectMapper.writeValueAsString(item);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize cart item", e);
        }
    }

    private CartItemResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, CartItemResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize cart item", e);
        }
    }

    private CartResponse buildCartResponse(UUID userId) {
        String cartKey = cartKey(userId);

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey);

        List<CartItemResponse> items = entries.values().stream()
                .map(v -> deserialize((String) v))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String appliedCoupon = redisTemplate.opsForValue()
                .get(couponKey(userId));

        // Discount is calculated at checkout — not here.
        // Cart response shows coupon code only.
        // Actual discount amount comes from seller-service validation.
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        return CartResponse.builder()
                .items(items)
                .totalItems(items.size())
                .totalAmount(totalAmount)
                .appliedCoupon(appliedCoupon)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }
}