package com.shopflow.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerServiceClient {

    @GetMapping("/api/sellers/internal/{sellerId}/details")
    SellerInternalResponse getSellerById(@PathVariable("sellerId") UUID sellerId);
}