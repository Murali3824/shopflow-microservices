package com.shopflow.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/internal/{orderId}")
    OrderInternalResponse getOrderById(@PathVariable("orderId") UUID orderId);
}