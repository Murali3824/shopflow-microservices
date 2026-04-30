package com.shopflow.notification.service;

import com.shopflow.notification.event.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private void sendHtmlEmail(String to, String subject, String templateName,
                               Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlBody = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Email sent to {} | template={} | subject={}", to, templateName, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {} | template={} | error={}",
                    to, templateName, e.getMessage());
            throw new RuntimeException("Email sending failed for template: " + templateName, e);
        }
    }

    @Override
    public void sendOrderConfirmation(OrderPlacedEvent event) {
        sendHtmlEmail(
                event.getUserEmail(),
                "Order Placed Successfully — #" + event.getOrderId(),
                "order-confirmation",
                Map.of(
                        "fullName", event.getFullName(),
                        "orderId", event.getOrderId(),
                        "totalAmount", event.getTotalAmount(),
                        "paymentMethod", event.getPaymentMethod()
                )
        );
    }

    @Override
    public void sendPaymentReceipt(PaymentCompletedEvent event) {
        sendHtmlEmail(
                event.getUserEmail(),
                "Payment Confirmed — Order #" + event.getOrderId(),
                "payment-receipt",
                Map.of(
                        "fullName", event.getFullName(),
                        "orderId", event.getOrderId(),
                        "amount", event.getAmount(),
                        "gateway", event.getGateway(),
                        "gatewayPaymentId", event.getGatewayPaymentId()
                )
        );
    }

    @Override
    public void sendOrderShipped(OrderShippedEvent event) {
        sendHtmlEmail(
                event.getUserEmail(),
                "Your Order Has Been Shipped — #" + event.getOrderId(),
                "order-shipped",
                Map.of(
                        "fullName", event.getFullName(),
                        "orderId", event.getOrderId(),
                        "trackingNumber", event.getTrackingNumber()
                )
        );
    }

    @Override
    public void sendOrderDelivered(OrderDeliveredEvent event) {
        sendHtmlEmail(
                event.getUserEmail(),
                "Your Order Has Been Delivered — #" + event.getOrderId(),
                "order-delivered",
                Map.of(
                        "fullName", event.getFullName(),
                        "orderId", event.getOrderId()
                )
        );
    }

    @Override
    public void sendRefundInitiated(ReturnApprovedEvent event) {
        sendHtmlEmail(
                event.getUserEmail(),
                "Refund Initiated — Order #" + event.getOrderId(),
                "refund-initiated",
                Map.of(
                        "fullName", event.getFullName(),
                        "orderId", event.getOrderId(),
                        "refundAmount", event.getAmount()
                )
        );
    }

    @Override
    public void sendLowStockAlert(ProductLowStockEvent event) {
        sendHtmlEmail(
                event.getSellerEmail(),
                "Low Stock Alert — " + event.getProductName(),
                "low-stock-alert",
                Map.of(
                        "sellerName", event.getSellerName(),
                        "productName", event.getProductName(),
                        "skuCode", event.getSkuCode(),
                        "currentStock", event.getCurrentStock(),
                        "lowStockThreshold", event.getLowStockThreshold()
                )
        );
    }

    @Override
    public void sendOtpEmail(OtpRequestedEvent event) {

        String subject = switch (event.getOtpType()) {
            case REGISTRATION -> "Verify your email";
            case RESEND -> "Your OTP (Resent)";
            case PASSWORD_RESET -> "Reset your password";
        };

        sendHtmlEmail(
                event.getEmail(),
                subject,
                "otp-email",
                Map.of(
                        "name", event.getFullName(),
                        "otp", event.getOtp()
                )
        );
    }
}