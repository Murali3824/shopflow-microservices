package com.shopflow.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.shopflow.payment.client.OrderInternalResponse;
import com.shopflow.payment.client.OrderServiceClient;
import com.shopflow.payment.client.UserServiceClient;
import com.shopflow.payment.dto.*;
import com.shopflow.payment.entity.*;
import com.shopflow.payment.event.PaymentCompletedEvent;
import com.shopflow.payment.event.ReturnApprovedEvent;
import com.shopflow.payment.exception.*;
import com.shopflow.payment.repository.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SellerEarningRepository sellerEarningRepository;
    private final RefundRepository refundRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserServiceClient userServiceClient;
    private final OrderServiceClient orderServiceClient;

    @Value("${payment.razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${payment.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Value("${payment.stripe.secret-key}")
    private String stripeSecretKey;

    private static final String TOPIC_PAYMENT_COMPLETED = "payment.completed";

    // ---------------------------------------------------------------
    // INITIATE PAYMENT
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentInitiateResponse initiatePayment(UUID userId, PaymentInitiateRequest request) {

        String idempotencyKey = "order-" + request.getOrderId();

        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            throw new PaymentAlreadyExistsException(
                    "Payment already initiated for order: " + request.getOrderId());
        });

        Gateway gateway = parseGateway(request.getGateway());

        // Validate gateway matches order's payment method
        OrderInternalResponse order = orderServiceClient.getOrderById(request.getOrderId());
        if (!order.getPaymentMethod().equalsIgnoreCase(request.getGateway())) {
            throw new InvalidGatewayException(
                    "Gateway mismatch. Order was placed with " + order.getPaymentMethod()
                            + " but payment initiated with " + request.getGateway());
        }

        if (gateway == Gateway.COD) {
            return initiateCod(userId, request, idempotencyKey);
        } else if (gateway == Gateway.RAZORPAY) {
            return initiateRazorpay(userId, request, idempotencyKey);
        } else {
            return initiateStripe(userId, request, idempotencyKey);
        }
    }

    private PaymentInitiateResponse initiateCod(UUID userId,
                                                PaymentInitiateRequest request,
                                                String idempotencyKey) {
        Payment payment = Payment.builder()
                .orderId(request.getOrderId())
                .userId(userId)
                .gateway(Gateway.COD.name())
                .status(PaymentStatus.PENDING.name())
                .amount(request.getAmount())
                .idempotencyKey(idempotencyKey)
                .build();

        Payment saved = paymentRepository.save(payment);

        return PaymentInitiateResponse.builder()
                .paymentId(saved.getId())
                .orderId(saved.getOrderId())
                .gateway(saved.getGateway())
                .amount(saved.getAmount())
                .status(saved.getStatus())
                .build();
    }

    private PaymentInitiateResponse initiateRazorpay(UUID userId,
                                                     PaymentInitiateRequest request,
                                                     String idempotencyKey) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", request.getAmount()
                    .multiply(BigDecimal.valueOf(100)).intValue());
            orderRequest.put("currency", "INR");
            String receipt = "ord-" + request.getOrderId().toString().replace("-", "").substring(0, 32);
orderRequest.put("receipt", receipt);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String gatewayOrderId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .userId(userId)
                    .gateway(Gateway.RAZORPAY.name())
                    .gatewayOrderId(gatewayOrderId)
                    .status(PaymentStatus.PENDING.name())
                    .amount(request.getAmount())
                    .idempotencyKey(idempotencyKey)
                    .build();

            Payment saved = paymentRepository.save(payment);

            return PaymentInitiateResponse.builder()
                    .paymentId(saved.getId())
                    .orderId(saved.getOrderId())
                    .gateway(saved.getGateway())
                    .gatewayOrderId(gatewayOrderId)
                    .amount(saved.getAmount())
                    .status(saved.getStatus())
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new InvalidGatewayException("Razorpay order creation failed: " + e.getMessage());
        }
    }

    private PaymentInitiateResponse initiateStripe(UUID userId,
                                                   PaymentInitiateRequest request,
                                                   String idempotencyKey) {
        try {
            Stripe.apiKey = stripeSecretKey;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount()
                            .multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("inr")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = Payment.builder()
                    .orderId(request.getOrderId())
                    .userId(userId)
                    .gateway(Gateway.STRIPE.name())
                    .gatewayOrderId(intent.getId())
                    .status(PaymentStatus.PENDING.name())
                    .amount(request.getAmount())
                    .idempotencyKey(idempotencyKey)
                    .build();

            Payment saved = paymentRepository.save(payment);

            return PaymentInitiateResponse.builder()
                    .paymentId(saved.getId())
                    .orderId(saved.getOrderId())
                    .gateway(saved.getGateway())
                    .gatewayOrderId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .amount(saved.getAmount())
                    .status(saved.getStatus())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment intent creation failed: {}", e.getMessage());
            throw new InvalidGatewayException("Stripe payment intent creation failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // VERIFY PAYMENT
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request) {

        if (request.getPaymentId() == null) {
            throw new PaymentVerificationException("Payment ID is missing");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + request.getPaymentId()));

        // Idempotency check
        if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            return toPaymentResponse(payment);
        }

        Gateway gateway = parseGateway(payment.getGateway());

        switch (gateway) {

            case STRIPE -> {
                try {
                    Stripe.apiKey = stripeSecretKey;

                    // Confirm with test card
                    PaymentIntent intent = PaymentIntent.retrieve(request.getGatewayPaymentId());

                    if (!"succeeded".equals(intent.getStatus())) {
                        // Confirm with test payment method
                        PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                                .setPaymentMethod("pm_card_visa")
                                .setReturnUrl("https://example.com")
                                .build();
                        intent = intent.confirm(confirmParams);
                    }

                    if (!"succeeded".equals(intent.getStatus())) {
                        throw new PaymentVerificationException("Stripe payment not successful");
                    }

                } catch (StripeException e) {
                    throw new PaymentVerificationException(
                            "Stripe verification failed: " + e.getMessage());
                }
            }

            case RAZORPAY -> {
                verifyRazorpaySignature(
                        payment.getGatewayOrderId(),
                        request.getGatewayPaymentId(),
                        request.getRazorpaySignature()
                );
            }

            case COD -> {
                // No verification needed for COD
            }

            default -> throw new InvalidGatewayException(
                    "Unsupported gateway for verification: " + gateway);
        }

        payment.setGatewayPaymentId(request.getGatewayPaymentId());
        payment.setStatus(PaymentStatus.SUCCESS.name());
        payment.setPaidAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);

        publishPaymentCompleted(saved);

        return toPaymentResponse(saved);
    }

    private void verifyRazorpaySignature(String gatewayOrderId,
                                         String gatewayPaymentId,
                                         String signature) {
        if (signature == null || signature.isBlank()) {
            throw new PaymentVerificationException("Razorpay signature is missing");
        }
        try {
            String payload = gatewayOrderId + "|" + gatewayPaymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);

            if (!generated.equals(signature)) {
                throw new PaymentVerificationException("Razorpay signature verification failed");
            }
        } catch (PaymentVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentVerificationException("Signature verification error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // GET PAYMENT
    // ---------------------------------------------------------------

    @Override
    public PaymentResponse getPayment(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for order: " + orderId));
        return toPaymentResponse(payment);
    }

    // ---------------------------------------------------------------
    // REFUND PAYMENT
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public RefundResponse refundPayment(UUID paymentId, RefundRequest request) {

        if (refundRepository.existsByReturnRequestId(request.getReturnRequestId())) {
            throw new RefundException("Refund already processed for return request: "
                    + request.getReturnRequestId());
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId));

        if (!PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            throw new RefundException("Cannot refund a payment that is not successful");
        }

        Gateway gateway = parseGateway(payment.getGateway());

        if (gateway == Gateway.COD) {
            return processCodRefund(payment, request);
        } else if (gateway == Gateway.RAZORPAY) {
            return processRazorpayRefund(payment, request);
        } else {
            return processStripeRefund(payment, request);
        }
    }

    private RefundResponse processCodRefund(Payment payment, RefundRequest request) {
        com.shopflow.payment.entity.Refund refund =
                com.shopflow.payment.entity.Refund.builder()
                        .payment(payment)
                        .returnRequestId(request.getReturnRequestId())
                        .amount(payment.getAmount())
                        .status(RefundStatus.COMPLETED.name())
                        .build();

        com.shopflow.payment.entity.Refund saved = refundRepository.save(refund);
        return toRefundResponse(saved);
    }

    private RefundResponse processRazorpayRefund(Payment payment, RefundRequest request) {
        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", payment.getAmount()
                    .multiply(BigDecimal.valueOf(100)).intValue());

            com.razorpay.Refund razorpayRefund =
                    razorpayClient.payments.refund(payment.getGatewayPaymentId(), refundRequest);

            String gatewayRefundId = razorpayRefund.get("id");

            com.shopflow.payment.entity.Refund refund =
                    com.shopflow.payment.entity.Refund.builder()
                            .payment(payment)
                            .returnRequestId(request.getReturnRequestId())
                            .amount(payment.getAmount())
                            .gatewayRefundId(gatewayRefundId)
                            .status(RefundStatus.INITIATED.name())
                            .build();

            com.shopflow.payment.entity.Refund saved = refundRepository.save(refund);
            return toRefundResponse(saved);

        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw new RefundException("Razorpay refund failed: " + e.getMessage());
        }
    }

    private RefundResponse processStripeRefund(Payment payment, RefundRequest request) {
        try {
            Stripe.apiKey = stripeSecretKey;

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getGatewayPaymentId())
                    .build();

            com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.create(params);

            com.shopflow.payment.entity.Refund refund =
                    com.shopflow.payment.entity.Refund.builder()
                            .payment(payment)
                            .returnRequestId(request.getReturnRequestId())
                            .amount(payment.getAmount())
                            .gatewayRefundId(stripeRefund.getId())
                            .status(RefundStatus.INITIATED.name())
                            .build();

            com.shopflow.payment.entity.Refund saved = refundRepository.save(refund);
            return toRefundResponse(saved);

        } catch (StripeException e) {
            log.error("Stripe refund failed: {}", e.getMessage());
            throw new RefundException("Stripe refund failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // WEBHOOKS
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void handleRazorpayWebhook(String payload, String signature) {
        try {
            verifyRazorpayWebhookSignature(payload, signature);

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = json
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String gatewayOrderId = paymentEntity.getString("order_id");
                String gatewayPaymentId = paymentEntity.getString("id");

                paymentRepository.findByGatewayOrderId(gatewayOrderId)
                        .ifPresent(payment -> {

                            if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                                return;
                            }

                            payment.setGatewayPaymentId(gatewayPaymentId);
                            payment.setStatus(PaymentStatus.SUCCESS.name());
                            payment.setPaidAt(LocalDateTime.now());

                            Payment saved = paymentRepository.save(payment);

                            publishPaymentCompleted(saved);
                        });
            }
        } catch (Exception e) {
            log.error("Razorpay webhook processing failed: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        try {
            JSONObject json = new JSONObject(payload);
            String eventType = json.getString("type");

            if ("payment_intent.succeeded".equals(eventType)) {
                JSONObject intentData = json
                        .getJSONObject("data")
                        .getJSONObject("object");

                String paymentIntentId = intentData.getString("id");

                paymentRepository.findByGatewayOrderId(paymentIntentId)
                        .ifPresent(payment -> {

                            if (PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
                                return; // idempotency
                            }

                            payment.setGatewayPaymentId(paymentIntentId);
                            payment.setStatus(PaymentStatus.SUCCESS.name());
                            payment.setPaidAt(LocalDateTime.now());

                            Payment saved = paymentRepository.save(payment);

                            publishPaymentCompleted(saved);
                        });
            }
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: {}", e.getMessage());
        }
    }

    private void verifyRazorpayWebhookSignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String generated = HexFormat.of().formatHex(hash);

            if (!generated.equals(signature)) {
                throw new PaymentVerificationException("Invalid Razorpay webhook signature");
            }
        } catch (PaymentVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentVerificationException("Webhook signature error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // KAFKA
    // ---------------------------------------------------------------

    private void publishPaymentCompleted(Payment payment) {
        try {

            // STEP 1: Fetch user details
            UserServiceClient.UserProfileResponse userProfile =
                    userServiceClient.getUserProfile(
                            payment.getUserId(),
                            payment.getUserId().toString(),
                            "USER"
                    );

            // STEP 2: Build JSON with ALL required fields
            String message = String.format(
                    "{\"orderId\":\"%s\",\"userId\":\"%s\",\"userEmail\":\"%s\",\"fullName\":\"%s\",\"amount\":%s,\"gateway\":\"%s\",\"gatewayPaymentId\":\"%s\"}",
                    payment.getOrderId(),
                    payment.getUserId(),
                    userProfile.getEmail(),
                    userProfile.getFullName(),
                    payment.getAmount(),
                    payment.getGateway(),
                    payment.getGatewayPaymentId() != null
                            ? payment.getGatewayPaymentId()
                            : "COD-" + payment.getOrderId()
            );

            // STEP 3: Send to Kafka
            kafkaTemplate.send(
                    TOPIC_PAYMENT_COMPLETED,
                    payment.getOrderId().toString(),
                    message
            );

        } catch (Exception e) {
            log.error("Failed to publish payment.completed event: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // MAPPERS
    // ---------------------------------------------------------------

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .userId(payment.getUserId())
                .gateway(payment.getGateway())
                .gatewayOrderId(payment.getGatewayOrderId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private RefundResponse toRefundResponse(com.shopflow.payment.entity.Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment().getId())
                .returnRequestId(refund.getReturnRequestId())
                .amount(refund.getAmount())
                .gatewayRefundId(refund.getGatewayRefundId())
                .status(refund.getStatus())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    @Override
    public List<PaymentResponse> getMyPayments(UUID userId) {
        return paymentRepository.findAllByUserId(userId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    @Override
    public SellerEarningsSummary getSellerEarnings(UUID sellerId) {
        List<SellerEarning> earnings = sellerEarningRepository.findAllBySellerId(sellerId);

        BigDecimal totalNet = sellerEarningRepository.sumNetEarningBySellerId(sellerId);

        BigDecimal totalGross = earnings.stream()
                .map(SellerEarning::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCommission = earnings.stream()
                .map(SellerEarning::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SellerEarningResponse> earningResponses = earnings.stream()
                .map(this::toSellerEarningResponse)
                .toList();

        return SellerEarningsSummary.builder()
                .sellerId(sellerId)
                .totalNetEarning(totalNet)
                .totalCommissionPaid(totalCommission)
                .totalGrossAmount(totalGross)
                .earnings(earningResponses)
                .build();
    }

    // ---------------------------------------------------------------
// INTERNAL — called by admin-service via Feign
// ---------------------------------------------------------------

    @Override
    @Transactional
    public void triggerRefundInternal(UUID paymentId, UUID returnRequestId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found: " + paymentId));

        if (!PaymentStatus.SUCCESS.name().equals(payment.getStatus())) {
            throw new RefundException(
                    "Cannot refund a payment that is not successful");
        }

        Gateway gateway = parseGateway(payment.getGateway());

        String gatewayRefundId = null;

        if (gateway == Gateway.COD) {

            com.shopflow.payment.entity.Refund refund =
                    com.shopflow.payment.entity.Refund.builder()
                            .payment(payment)
                            .returnRequestId(returnRequestId)
                            .amount(payment.getAmount())
                            .status(RefundStatus.COMPLETED.name())
                            .build();
            refundRepository.save(refund);

        } else if (gateway == Gateway.RAZORPAY) {

            try {
                RazorpayClient razorpayClient =
                        new RazorpayClient(razorpayKeyId, razorpayKeySecret);

                JSONObject refundRequest = new JSONObject();
                refundRequest.put("amount", payment.getAmount()
                        .multiply(BigDecimal.valueOf(100)).intValue());

                com.razorpay.Refund razorpayRefund =
                        razorpayClient.payments.refund(
                                payment.getGatewayPaymentId(), refundRequest);

                gatewayRefundId = razorpayRefund.get("id");

                com.shopflow.payment.entity.Refund refund =
                        com.shopflow.payment.entity.Refund.builder()
                                .payment(payment)
                                .returnRequestId(returnRequestId)
                                .amount(payment.getAmount())
                                .gatewayRefundId(gatewayRefundId)
                                .status(RefundStatus.INITIATED.name())
                                .build();
                refundRepository.save(refund);

            } catch (RazorpayException e) {
                throw new RefundException(
                        "Razorpay refund failed: " + e.getMessage());
            }

        } else {

            try {
                Stripe.apiKey = stripeSecretKey;

                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(payment.getGatewayPaymentId())
                        .build();

                com.stripe.model.Refund stripeRefund =
                        com.stripe.model.Refund.create(params);

                gatewayRefundId = stripeRefund.getId();

                com.shopflow.payment.entity.Refund refund =
                        com.shopflow.payment.entity.Refund.builder()
                                .payment(payment)
                                .returnRequestId(returnRequestId)
                                .amount(payment.getAmount())
                                .gatewayRefundId(gatewayRefundId)
                                .status(RefundStatus.INITIATED.name())
                                .build();
                refundRepository.save(refund);

            } catch (StripeException e) {
                throw new RefundException(
                        "Stripe refund failed: " + e.getMessage());
            }
        }

        // ── Enrich with user details and fire Kafka event ──────────────
        try {
            UserServiceClient.UserProfileResponse userProfile =
                    userServiceClient.getUserProfile(
                            payment.getUserId(),
                            payment.getUserId().toString(),
                            "USER"
                    );

            ReturnApprovedEvent event = ReturnApprovedEvent.builder()
                    .orderId(payment.getOrderId())
                    .userId(payment.getUserId())
                    .userEmail(userProfile.getEmail())
                    .fullName(userProfile.getFullName())
                    .amount(payment.getAmount())
                    .approvedAt(LocalDateTime.now())
                    .build();

            // KafkaTemplate is <String, String> — serialize manually
            String eventJson = new com.fasterxml.jackson.databind.ObjectMapper()
                    .findAndRegisterModules()
                    .writeValueAsString(event);

            kafkaTemplate.send(
                    "return.approved",
                    payment.getOrderId().toString(),
                    eventJson
            );

            log.info("return.approved Kafka event fired — orderId={} userId={}",
                    payment.getOrderId(), payment.getUserId());

        } catch (Exception e) {
            log.error("Failed to publish return.approved event for paymentId={}: {}",
                    paymentId, e.getMessage());
        }

        log.info("Internal refund triggered — paymentId={}", paymentId);
    }

    @Override
    public RevenueReportResponse getRevenueReport(ZonedDateTime from, ZonedDateTime to) {

        LocalDateTime fromLocal = from.toLocalDateTime();
        LocalDateTime toLocal   = to.toLocalDateTime();

        BigDecimal totalRevenue    = sellerEarningRepository
                .sumGrossAmountBetween(fromLocal, toLocal);
        BigDecimal totalCommission = sellerEarningRepository
                .sumCommissionAmountBetween(fromLocal, toLocal);
        Long totalOrders           = sellerEarningRepository
                .countDistinctOrdersBetween(fromLocal, toLocal);

        return RevenueReportResponse.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .totalCommission(totalCommission)
                .platformRevenue(totalCommission)   // platform earns the commission
                .period(from.toLocalDate() + " to " + to.toLocalDate())
                .build();
    }

    private SellerEarningResponse toSellerEarningResponse(SellerEarning earning) {
        return SellerEarningResponse.builder()
                .id(earning.getId())
                .sellerId(earning.getSellerId())
                .orderId(earning.getOrderId())
                .orderItemId(earning.getOrderItemId())
                .grossAmount(earning.getGrossAmount())
                .commissionRate(earning.getCommissionRate())
                .commissionAmount(earning.getCommissionAmount())
                .netEarning(earning.getNetEarning())
                .createdAt(earning.getCreatedAt())
                .build();
    }

    // ---------------------------------------------------------------
    // HELPERS
    // ---------------------------------------------------------------

    private Gateway parseGateway(String value) {
        try {
            return Gateway.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidGatewayException("Unsupported gateway: " + value);
        }
    }
}