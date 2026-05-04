package com.shopflow.admin.controller;

import com.shopflow.admin.dto.*;
import com.shopflow.admin.security.HeaderAuthFilter;
import com.shopflow.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<OrderResponse> orders = adminService.getAllOrders(pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders fetched successfully", orders));
    }

    @GetMapping("/returns")
    public ResponseEntity<ApiResponse<Page<ReturnRequestResponse>>> getAllReturnRequests(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ReturnRequestResponse> returns = adminService.getAllReturnRequests(pageable);
        return ResponseEntity.ok(ApiResponse.success("Return requests fetched successfully", returns));
    }

    @PutMapping("/returns/{returnId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveReturn(
            @PathVariable UUID returnId,
            @RequestBody ApproveReturnRequest approveReturnRequest,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.approveReturn(adminId, returnId, approveReturnRequest);
        return ResponseEntity.ok(ApiResponse.success("Return request approved successfully"));
    }

    @PutMapping("/returns/{returnId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectReturn(
            @PathVariable UUID returnId,
            @RequestBody ApproveReturnRequest approveReturnRequest,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.rejectReturn(adminId, returnId, approveReturnRequest);
        return ResponseEntity.ok(ApiResponse.success("Return request rejected successfully"));
    }
}