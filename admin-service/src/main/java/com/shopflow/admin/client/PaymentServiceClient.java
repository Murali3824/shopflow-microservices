package com.shopflow.admin.client;

import com.shopflow.admin.dto.PaymentResponse;
import com.shopflow.admin.dto.RevenueReportResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/api/payments/internal/{paymentId}/refund")
    void triggerRefund(
            @PathVariable("paymentId") UUID paymentId,
            @RequestParam("returnRequestId") UUID returnRequestId);

    @GetMapping("/api/payments/internal/revenue")
    RevenueReportResponse getRevenueReport(
            @RequestParam("from") ZonedDateTime from,
            @RequestParam("to") ZonedDateTime to);

    @GetMapping("/api/payments/internal/order/{orderId}")
    PaymentResponse getPaymentByOrderId(
            @PathVariable("orderId") UUID orderId);

}