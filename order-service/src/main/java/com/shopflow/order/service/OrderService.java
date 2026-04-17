package com.shopflow.order.service;

import com.shopflow.order.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse placeOrder(UUID userId, OrderRequest request);

    OrderResponse getOrder(UUID userId, UUID orderId);

    List<OrderResponse> listOrders(UUID userId, int page, int size);

    OrderResponse cancelOrder(UUID userId, UUID orderId);

    OrderResponse markShipped(UUID sellerId, UUID orderId, String trackingNumber);

    OrderResponse markDelivered(UUID orderId);

    ReturnRequestResponse raiseReturn(UUID userId, UUID orderId,
                                      ReturnRequestRequest request);

    OrderInternalResponse getOrderInternal(UUID orderId);

    boolean verifyPurchase(UUID orderId, UUID userId, UUID productId);

    Page<OrderResponse> getAllOrders(Pageable pageable);
    Page<ReturnRequestResponse> getAllReturnRequests(Pageable pageable);
    void approveReturn(UUID returnId, String note);
    void rejectReturn(UUID returnId, String note);
    ReturnRequestResponse getReturnById(UUID returnId);
}