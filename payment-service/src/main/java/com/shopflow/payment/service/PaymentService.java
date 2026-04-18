package com.shopflow.payment.service;

import com.shopflow.payment.dto.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentInitiateResponse initiatePayment(UUID userId, PaymentInitiateRequest request);

    PaymentResponse verifyPayment(PaymentVerifyRequest request);

    PaymentResponse getPayment(UUID orderId);

    RefundResponse refundPayment(UUID paymentId, RefundRequest request);

    void handleRazorpayWebhook(String payload, String signature);

    void handleStripeWebhook(String payload, String signature);

    List<PaymentResponse> getMyPayments(UUID userId);

    SellerEarningsSummary getSellerEarnings(UUID sellerId);

    // ── Internal endpoints for admin-service ──────────────────────
    void triggerRefundInternal(UUID paymentId, UUID returnRequestId);

    RevenueReportResponse getRevenueReport(ZonedDateTime from, ZonedDateTime to);
}