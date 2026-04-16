package com.shopflow.product.client;

import lombok.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "seller-service", path = "/api/sellers")
public interface SellerServiceClient {

    @GetMapping("/internal/{sellerId}")
    SellerValidationResponse getSellerById(@PathVariable UUID sellerId);

    @GetMapping("/internal/{sellerId}/details")
    SellerInternalResponse getSellerDetails(@PathVariable UUID sellerId);


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SellerInternalResponse {
        private UUID userId;
        private String email;
        private String fullName;
    }
}