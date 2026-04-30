package com.shopflow.notification.service;

import com.shopflow.notification.event.*;

public interface EmailService {

    void sendOrderConfirmation(OrderPlacedEvent event);
    void sendPaymentReceipt(PaymentCompletedEvent event);
    void sendOrderShipped(OrderShippedEvent event);
    void sendOrderDelivered(OrderDeliveredEvent event);
    void sendRefundInitiated(ReturnApprovedEvent event);
    void sendLowStockAlert(ProductLowStockEvent event);
    void sendOtpEmail(OtpRequestedEvent event);
}