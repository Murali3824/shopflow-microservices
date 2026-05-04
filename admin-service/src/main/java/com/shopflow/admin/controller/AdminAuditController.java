package com.shopflow.admin.controller;

import com.shopflow.admin.dto.ApiResponse;
import com.shopflow.admin.dto.AuditLogResponse;
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
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "performedAt") Pageable pageable,
            HttpServletRequest request) {

        UUID adminId = HeaderAuthFilter.extractAdminId(request);
        Page<AuditLogResponse> logs = adminService.getAuditLogs(adminId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs fetched successfully", logs));
    }
}