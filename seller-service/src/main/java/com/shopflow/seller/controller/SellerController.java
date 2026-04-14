package com.shopflow.seller.controller;

import com.shopflow.seller.dto.*;
import com.shopflow.seller.repository.SellerRepository;
import com.shopflow.seller.service.SellerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;
    private final SellerRepository sellerRepository;

    // ─── Register ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<SellerResponse> register(
            @Valid @RequestBody SellerRegistrationRequest request,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        SellerResponse response = sellerService.registerSeller(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<SellerResponse> getProfile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(sellerService.getSellerProfile(userId));
    }

    // ─── Store Update ─────────────────────────────────────────────────────────

    @PutMapping("/me/store")
    public ResponseEntity<SellerResponse> updateStore(
            @Valid @RequestBody StoreUpdateRequest request,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(sellerService.updateStore(userId, request));
    }

    // ─── Earnings ─────────────────────────────────────────────────────────────

    @GetMapping("/me/earnings")
    public ResponseEntity<BigDecimal> getEarnings(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(sellerService.getEarnings(userId));
    }

    // ─── Coupons ──────────────────────────────────────────────────────────────

    @PostMapping("/me/coupons")
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponRequest request,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        CouponResponse response = sellerService.createCoupon(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/coupons")
    public ResponseEntity<List<CouponResponse>> getCoupons(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(sellerService.getCoupons(userId));
    }

    @DeleteMapping("/me/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = (UUID) authentication.getPrincipal();
        sellerService.deleteCoupon(userId, id);
        return ResponseEntity.noContent().build();
    }


    // Internal endpoint — called by product-service via Feign
// to validate seller exists and is APPROVED before listing products.
// Not exposed in public API documentation.
    @GetMapping("/internal/{sellerId}")
    public ResponseEntity<SellerValidationResponse> getSellerById(
            @PathVariable UUID sellerId) {

        return sellerRepository.findByUserId(sellerId)
                .map(seller -> ResponseEntity.ok(
                        new SellerValidationResponse(seller.getUserId(), seller.getStatus().name())
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/sellers/internal/coupons/validate
    @GetMapping("/internal/coupons/validate")
    public ResponseEntity<CouponValidationResponse> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal) {
        return ResponseEntity.ok(
                sellerService.validateCoupon(code, orderTotal));
    }


    // In SellerController — add this endpoint
    @GetMapping("/internal/{sellerId}/details")
    public ResponseEntity<SellerInternalResponse> getSellerInternal(
            @PathVariable UUID sellerId) {
        return ResponseEntity.ok(sellerService.getSellerInternal(sellerId));
    }

    // ─── Admin Internal Endpoints ─────────────────────────────────────────────
// Called by admin-service via Feign only.
// No JWT required — whitelisted in SecurityConfig.

    @GetMapping("/internal/all")
    public ResponseEntity<Page<SellerResponse>> getAllSellers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(sellerService.getAllSellers(pageable));
    }

    @GetMapping("/internal/pending")
    public ResponseEntity<List<SellerResponse>> getPendingSellers() {
        return ResponseEntity.ok(sellerService.getPendingSellers());
    }

    @PutMapping("/internal/{sellerId}/approve")
    public ResponseEntity<Void> approveSeller(@PathVariable UUID sellerId) {
        sellerService.approveSeller(sellerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/internal/{sellerId}/reject")
    public ResponseEntity<Void> rejectSeller(@PathVariable UUID sellerId) {
        sellerService.rejectSeller(sellerId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/internal/{sellerId}/commission")
    public ResponseEntity<Void> updateCommission(
            @PathVariable UUID sellerId,
            @RequestBody @Valid UpdateCommissionRequest request) {
        sellerService.updateCommission(sellerId,
                BigDecimal.valueOf(request.getCommissionRate()));
        return ResponseEntity.ok().build();
    }








}

