package com.shopflow.admin.service;

import com.shopflow.admin.client.*;
import com.shopflow.admin.dto.*;
import com.shopflow.admin.exception.BusinessException;
import com.shopflow.admin.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserServiceClient userServiceClient;
    private final SellerServiceClient  sellerServiceClient;
    private final ProductServiceClient productServiceClient;
    private final OrderServiceClient   orderServiceClient;
    private final PaymentServiceClient paymentServiceClient;
    private final AuditLogService      auditLogService;

    // ─────────────────────────────────────────────
    // User management
    // ─────────────────────────────────────────────

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        try {
            return userServiceClient.getAllUsers(pageable);
        } catch (FeignException e) {
            log.error("user-service unavailable while fetching all users: {}", e.getMessage());
            throw new ServiceUnavailableException("user-service is currently unavailable");
        }
    }

    @Override
    public void banUser(UUID adminId, UUID userId) {
        try {
            userServiceClient.banUser(userId);
        } catch (FeignException e) {
            log.error("user-service unavailable while banning user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("user-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "BAN_USER", "USER", userId,
                "User banned by admin");
    }

    @Override
    public void unbanUser(UUID adminId, UUID userId) {
        try {
            userServiceClient.unbanUser(userId);
        } catch (FeignException e) {
            log.error("user-service unavailable while unbanning user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("user-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "UNBAN_USER", "USER", userId,
                "User unbanned by admin");
    }

    // ─────────────────────────────────────────────
    // Seller management
    // ─────────────────────────────────────────────

    @Override
    public Page<SellerResponse> getAllSellers(Pageable pageable) {
        try {
            return sellerServiceClient.getAllSellers(pageable);
        } catch (FeignException e) {
            log.error("seller-service unavailable while fetching all sellers: {}", e.getMessage());
            throw new ServiceUnavailableException("seller-service is currently unavailable");
        }
    }

    @Override
    public List<SellerResponse> getPendingSellers() {
        try {
            return sellerServiceClient.getPendingSellers();
        } catch (FeignException e) {
            log.error("seller-service unavailable while fetching pending sellers: {}", e.getMessage());
            throw new ServiceUnavailableException("seller-service is currently unavailable");
        }
    }

    @Override
    public void approveSeller(UUID adminId, UUID sellerId) {
        try {
            sellerServiceClient.approveSeller(sellerId);
        } catch (FeignException e) {
            log.error("seller-service unavailable while approving seller {}: {}", sellerId, e.getMessage());
            throw new ServiceUnavailableException("seller-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "APPROVE_SELLER", "SELLER", sellerId,
                "Seller account approved by admin");
    }

    @Override
    public void rejectSeller(UUID adminId, UUID sellerId) {
        try {
            sellerServiceClient.rejectSeller(sellerId);
        } catch (FeignException e) {
            log.error("seller-service unavailable while rejecting seller {}: {}", sellerId, e.getMessage());
            throw new ServiceUnavailableException("seller-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "REJECT_SELLER", "SELLER", sellerId,
                "Seller account rejected by admin");
    }

    @Override
    public void updateCommission(UUID adminId, UUID sellerId, UpdateCommissionRequest request) {
        try {
            sellerServiceClient.updateCommission(sellerId, request);
        } catch (FeignException e) {
            log.error("seller-service unavailable while updating commission for seller {}: {}",
                    sellerId, e.getMessage());
            throw new ServiceUnavailableException("seller-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "UPDATE_COMMISSION", "SELLER", sellerId,
                "rate=" + request.getCommissionRate() + "%");
    }

    // ─────────────────────────────────────────────
    // Product management
    // ─────────────────────────────────────────────

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        try {
            return productServiceClient.getAllProducts(pageable);
        } catch (FeignException e) {
            log.error("product-service unavailable while fetching all products: {}", e.getMessage());
            throw new ServiceUnavailableException("product-service is currently unavailable");
        }
    }

    @Override
    public void deleteProduct(UUID adminId, UUID productId) {
        try {
            productServiceClient.deleteProduct(productId);
        } catch (FeignException e) {
            log.error("product-service unavailable while deleting product {}: {}", productId, e.getMessage());
            throw new ServiceUnavailableException("product-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "DELETE_PRODUCT", "PRODUCT", productId,
                "Product deleted by admin");
    }

    // ─────────────────────────────────────────────
    // Order + Return management
    // ─────────────────────────────────────────────

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        try {
            return orderServiceClient.getAllOrders(pageable);
        } catch (FeignException e) {
            log.error("order-service unavailable while fetching all orders: {}", e.getMessage());
            throw new ServiceUnavailableException("order-service is currently unavailable");
        }
    }

    @Override
    public Page<ReturnRequestResponse> getAllReturnRequests(Pageable pageable) {
        try {
            return orderServiceClient.getAllReturns(pageable);
        } catch (FeignException e) {
            log.error("order-service unavailable while fetching return requests: {}", e.getMessage());
            throw new ServiceUnavailableException("order-service is currently unavailable");
        }
    }

    @Override
    public void approveReturn(UUID adminId, UUID returnId, ApproveReturnRequest request) {

        // Step 1 — fetch return to get orderId
        ReturnRequestResponse returnRequest;
        try {
            returnRequest = orderServiceClient.getReturnById(returnId);
        } catch (FeignException e) {
            log.error("order-service unavailable while fetching return {}: {}",
                    returnId, e.getMessage());
            throw new ServiceUnavailableException("order-service is currently unavailable");
        }

        // Step 2 — approve return in order-service
        // order-service fires return.approved Kafka event
        // payment-service consumer reverses seller earnings
        try {
            orderServiceClient.approveReturn(returnId, request);
        } catch (FeignException.Conflict | FeignException.BadRequest e) {
            throw new BusinessException("Cannot approve return: " + extractMessage(e));
        } catch (FeignException e) {
            log.error("order-service unavailable while approving return {}: {}",
                    returnId, e.getMessage());
            throw new ServiceUnavailableException("order-service is currently unavailable");
        }

        // Step 3 — fetch payment by orderId via internal endpoint
        UUID orderId = returnRequest.getOrderId();
        PaymentResponse payment;
        try {
            payment = paymentServiceClient.getPaymentByOrderId(orderId);
        } catch (FeignException e) {
            log.error("payment-service unavailable while fetching payment for order {}: {}",
                    orderId, e.getMessage());
            throw new ServiceUnavailableException("payment-service is currently unavailable");
        }

        // Step 4 — trigger refund via internal endpoint
        try {
            paymentServiceClient.triggerRefund(payment.getId(), returnId);
        } catch (FeignException e) {
            log.error("payment-service unavailable while triggering refund for payment {}: {}",
                    payment.getId(), e.getMessage());
            throw new ServiceUnavailableException("payment-service is currently unavailable");
        }

        auditLogService.logAction(adminId, "APPROVE_RETURN", "RETURN_REQUEST", returnId,
                "Return approved + refund triggered — orderId: " + orderId);
    }

    private String extractMessage(FeignException e) {
        try {
            String body = e.contentUTF8();
            if (body != null && body.contains("\"message\":\"")) {
                int start = body.indexOf("\"message\":\"") + 11;
                int end = body.indexOf("\"", start);
                return body.substring(start, end);
            }
        } catch (Exception ignored) {}
        return e.getMessage();
    }

    @Override
    public void rejectReturn(UUID adminId, UUID returnId, ApproveReturnRequest request) {
        try {
            orderServiceClient.rejectReturn(returnId, request);
        } catch (FeignException e) {
            log.error("order-service unavailable while rejecting return {}: {}", returnId, e.getMessage());
            throw new ServiceUnavailableException("order-service is currently unavailable");
        }
        auditLogService.logAction(adminId, "REJECT_RETURN", "RETURN_REQUEST", returnId,
                "Return rejected — note: " + request.getNote());
    }

    // ─────────────────────────────────────────────
    // Reports
    // ─────────────────────────────────────────────

    @Override
    public RevenueReportResponse getRevenueReport(ZonedDateTime from, ZonedDateTime to) {
        try {
            return paymentServiceClient.getRevenueReport(from, to);
        } catch (FeignException e) {
            log.error("payment-service unavailable while fetching revenue report: {}", e.getMessage());
            throw new ServiceUnavailableException("payment-service is currently unavailable");
        }
    }

    // ─────────────────────────────────────────────
    // Audit
    // ─────────────────────────────────────────────

    @Override
    public Page<AuditLogResponse> getAuditLogs(UUID adminId, Pageable pageable) {
        return auditLogService.getAuditLogs(adminId, pageable);
    }
}