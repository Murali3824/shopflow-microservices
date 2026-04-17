package com.shopflow.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@FeignClient(name = "seller-service")
public interface SellerServiceClient {

    @GetMapping("/api/sellers/internal/coupons/validate")
    CouponValidationResponse validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal
    );

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class CouponValidationResponse {
        private String code;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal discountAmount;
        private boolean valid;
    }
}