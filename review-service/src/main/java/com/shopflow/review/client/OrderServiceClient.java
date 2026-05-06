package com.shopflow.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/internal/{orderId}/verify-purchase")
    boolean verifyPurchase(
            @PathVariable("orderId") UUID orderId,
            @RequestParam("userId") UUID userId,
            @RequestParam("productId") UUID productId
    );
}