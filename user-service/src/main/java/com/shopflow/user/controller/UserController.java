package com.shopflow.user.controller;

import com.shopflow.user.dto.*;
import com.shopflow.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    // ── Extract userId from Security context ─────────────────
    // GatewayHeaderFilter stores UUID as principal — cast directly
    private UUID getUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }


    // ── Profile ──────────────────────────────────────────────

    // GET /api/users/profile
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(Authentication authentication) {
        return ResponseEntity.ok(
                userService.getProfile(getUserId(authentication))
        );
    }

    // PUT /api/users/profile
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(
                userService.updateProfile(getUserId(authentication), request)
        );
    }

    // POST /api/users/me/avatar
    @PostMapping(
            value = "/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(userService.uploadAvatar(getUserId(authentication), file));
    }


    // ── Addresses ────────────────────────────────────────────

    // GET /api/users/me/addresses
    @GetMapping("/addresses")
    public ResponseEntity<List<UserAddressResponse>> getAddresses(Authentication authentication) {
        return ResponseEntity.ok(
                userService.getAddresses(getUserId(authentication))
        );
    }

    // POST /api/users/me/addresses
    @PostMapping("/addresses")
    public ResponseEntity<UserAddressResponse> addAddress(
            Authentication authentication,
            @Valid @RequestBody UserAddressRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.addAddress(getUserId(authentication), request));
    }

    // PUT /api/users/me/addresses/{id}
    @PutMapping("/addresses/{id}")
    public ResponseEntity<UserAddressResponse> updateAddress(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UserAddressRequest request) {
        return ResponseEntity.ok(
                userService.updateAddress(getUserId(authentication), id, request)
        );
    }

    // DELETE /api/users/me/addresses/{id}
    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable UUID id) {
        userService.deleteAddress(getUserId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    // PUT /api/users/me/addresses/{id}/default
    @PutMapping("/addresses/{id}/default")
    public ResponseEntity<UserAddressResponse> setDefaultAddress(
            Authentication authentication,
            @PathVariable UUID id) {
        return ResponseEntity.ok(
                userService.setDefaultAddress(getUserId(authentication), id)
        );
    }


    // ── Internal endpoint for order-service ──────────────────────
    // Called by order-service via Feign to fetch default address
    // at order placement time for address snapshot.
    // Bypasses normal auth — trusts X-User-Id header from gateway.

    // GET /api/users/internal/{userId}
    @GetMapping("/internal/{userId}")
    public ResponseEntity<UserAddressResponse> getDefaultAddressForOrder(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getDefaultAddress(userId)
        );
    }

    // GET /api/users/internal/profile/{userId}
    // Called by order-service via Feign to fetch Profile details(Email,name)
    @GetMapping("/internal/profile/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfileForInternal(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(
                userService.getProfile(userId)
        );
    }


}