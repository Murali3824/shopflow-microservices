package com.shopflow.admin.service;

import com.shopflow.admin.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface AdminService {

    // User management
    Page<UserResponse> getAllUsers(Pageable pageable);
    void banUser(UUID adminId, UUID userId);
    void unbanUser(UUID adminId, UUID userId);

    // Seller management
    Page<SellerResponse> getAllSellers(Pageable pageable);
    List<SellerResponse> getPendingSellers();
    void approveSeller(UUID adminId, UUID sellerId);
    void rejectSeller(UUID adminId, UUID sellerId);
    void updateCommission(UUID adminId, UUID sellerId, UpdateCommissionRequest request);

    // Product management
    Page<ProductResponse> getAllProducts(Pageable pageable);
    void deleteProduct(UUID adminId, UUID productId);

    // Order + Return management
    Page<OrderResponse> getAllOrders(Pageable pageable);
    Page<ReturnRequestResponse> getAllReturnRequests(Pageable pageable);
    void approveReturn(UUID adminId, UUID returnId, ApproveReturnRequest request);
    void rejectReturn(UUID adminId, UUID returnId, ApproveReturnRequest request);

    // Reports
    RevenueReportResponse getRevenueReport(ZonedDateTime from, ZonedDateTime to);

    // Audit
    Page<AuditLogResponse> getAuditLogs(UUID adminId, Pageable pageable);
}