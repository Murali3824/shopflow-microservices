package com.shopflow.admin.client;

import com.shopflow.admin.dto.ApproveReturnRequest;
import com.shopflow.admin.dto.OrderResponse;
import com.shopflow.admin.dto.ReturnRequestResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/internal/all")
    Page<OrderResponse> getAllOrders(@SpringQueryMap Pageable pageable);

    @GetMapping("/api/orders/internal/returns")
    Page<ReturnRequestResponse> getAllReturns(@SpringQueryMap Pageable pageable);

    @PutMapping("/api/orders/internal/returns/{returnId}/approve")
    void approveReturn(@PathVariable("returnId") UUID returnId,
                       @RequestBody ApproveReturnRequest request);

    @PutMapping("/api/orders/internal/returns/{returnId}/reject")
    void rejectReturn(@PathVariable("returnId") UUID returnId,
                      @RequestBody ApproveReturnRequest request);

    @GetMapping("/api/orders/internal/returns/{returnId}")
    ReturnRequestResponse getReturnById(
            @PathVariable("returnId") UUID returnId);
}