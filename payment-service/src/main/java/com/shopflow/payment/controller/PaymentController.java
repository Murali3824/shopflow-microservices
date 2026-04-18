package com.shopflow.payment.controller;

import com.shopflow.payment.dto.*;
import com.shopflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PaymentInitiateRequest request) {

        PaymentInitiateResponse response = paymentService
                .initiatePayment(UUID.fromString(userId), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {

        PaymentResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID orderId) {

        PaymentResponse response = paymentService.getPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<RefundResponse> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {

        RefundResponse response = paymentService.refundPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        paymentService.handleRazorpayWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        paymentService.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(
            @RequestHeader("X-User-Id") String userId) {

        List<PaymentResponse> response = paymentService.getMyPayments(UUID.fromString(userId));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/internal/earnings/{sellerId}")
    public ResponseEntity<SellerEarningsSummary> getSellerEarnings(
            @PathVariable UUID sellerId) {

        SellerEarningsSummary response = paymentService.getSellerEarnings(sellerId);
        return ResponseEntity.ok(response);
    }

    // ── Internal — called by admin-service via Feign ──────────────────

    @PostMapping("/internal/{paymentId}/refund")
    public ResponseEntity<Void> triggerRefundInternal(
            @PathVariable UUID paymentId,
            @RequestParam UUID returnRequestId) {

        paymentService.triggerRefundInternal(paymentId, returnRequestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/internal/revenue")
    public ResponseEntity<RevenueReportResponse> getRevenueReport(
            @RequestParam ZonedDateTime from,
            @RequestParam ZonedDateTime to) {

        RevenueReportResponse response = paymentService.getRevenueReport(from, to);
        return ResponseEntity.ok(response);
    }

    // Add this endpoint
    @GetMapping("/internal/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderIdInternal(
            @PathVariable UUID orderId) {
        PaymentResponse response = paymentService.getPayment(orderId);
        return ResponseEntity.ok(response);
    }
}