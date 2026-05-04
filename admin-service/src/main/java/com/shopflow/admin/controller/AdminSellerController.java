package com.shopflow.admin.controller;

import com.shopflow.admin.dto.*;
import com.shopflow.admin.security.HeaderAuthFilter;
import com.shopflow.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/sellers")
@RequiredArgsConstructor
public class AdminSellerController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerResponse>>> getAllSellers(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<SellerResponse> sellers = adminService.getAllSellers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Sellers fetched successfully", sellers));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<SellerResponse>>> getPendingSellers() {
        List<SellerResponse> pending = adminService.getPendingSellers();
        return ResponseEntity.ok(ApiResponse.success("Pending sellers fetched successfully", pending));
    }

    @PutMapping("/{sellerId}/approve")
    public ResponseEntity<ApiResponse<Void>> approveSeller(
            @PathVariable UUID sellerId,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.approveSeller(adminId, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully"));
    }

    @PutMapping("/{sellerId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectSeller(
            @PathVariable UUID sellerId,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.rejectSeller(adminId, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Seller rejected successfully"));
    }

    @PutMapping("/{sellerId}/commission")
    public ResponseEntity<ApiResponse<Void>> updateCommission(
            @PathVariable UUID sellerId,
            @RequestBody @Valid UpdateCommissionRequest commissionRequest,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.updateCommission(adminId, sellerId, commissionRequest);
        return ResponseEntity.ok(ApiResponse.success("Commission rate updated successfully"));
    }
}