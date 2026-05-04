package com.shopflow.admin.controller;

import com.shopflow.admin.dto.ApiResponse;
import com.shopflow.admin.dto.UserResponse;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {

        Page<UserResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));
    }

    @PutMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable UUID userId,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.banUser(adminId, userId);
        return ResponseEntity.ok(ApiResponse.success("User banned successfully"));
    }

    @PutMapping("/{userId}/unban")
    public ResponseEntity<ApiResponse<Void>> unbanUser(
            @PathVariable UUID userId,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        adminService.unbanUser(adminId, userId);
        return ResponseEntity.ok(ApiResponse.success("User unbanned successfully"));
    }
}