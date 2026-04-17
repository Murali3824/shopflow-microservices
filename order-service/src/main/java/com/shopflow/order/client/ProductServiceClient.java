package com.shopflow.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @PostMapping("/api/products/internal/update-stock")
    void updateStock(@RequestBody StockUpdateRequest request);

    // New — validates SKU exists and returns real price + name
    @GetMapping("/api/products/internal/sku/{skuId}")
    SkuDetailsResponse getSkuDetails(@PathVariable UUID skuId);

    @Getter
    @Builder
    class StockUpdateRequest {
        private UUID skuId;
        private int quantity;
        private String operation;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SkuDetailsResponse {
        private UUID skuId;
        private UUID productId;
        private UUID sellerId;
        private String productName;
        private BigDecimal price;
        private Integer stockQty;
    }
}