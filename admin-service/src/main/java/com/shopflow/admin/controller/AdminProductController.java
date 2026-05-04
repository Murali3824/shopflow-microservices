package com.shopflow.admin.controller;

import com.shopflow.admin.dto.ApiResponse;
import com.shopflow.admin.dto.ProductResponse;
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
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductResponse> products = adminService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success("Products fetched successfully", products));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable UUID productId,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.deleteProduct(adminId, productId);
        return ResponseEntity.noContent().build();
    }
}