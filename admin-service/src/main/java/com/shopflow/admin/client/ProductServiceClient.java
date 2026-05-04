package com.shopflow.admin.client;

import com.shopflow.admin.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/internal/all")
    Page<ProductResponse> getAllProducts(@SpringQueryMap Pageable pageable);

    @DeleteMapping("/api/products/internal/{productId}")
    void deleteProduct(@PathVariable("productId") UUID productId);
}