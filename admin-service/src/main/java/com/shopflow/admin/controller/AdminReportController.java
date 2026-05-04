package com.shopflow.admin.controller;

import com.shopflow.admin.dto.ApiResponse;
import com.shopflow.admin.dto.RevenueReportResponse;
import com.shopflow.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminService adminService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime to) {

        RevenueReportResponse report = adminService.getRevenueReport(from, to);
        return ResponseEntity.ok(ApiResponse.success("Revenue report fetched successfully", report));
    }
}