package com.shopflow.auth.controller;

import com.shopflow.auth.dto.ResetPasswordRequest;
import com.shopflow.auth.dto.*;
import com.shopflow.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    // ── GET /api/auth/users/internal/{userId} ────────────────
// Internal endpoint — called by other services only
// Permitted without JWT in SecurityConfig

    @GetMapping("/users/internal/{userId}")
    public ResponseEntity<InternalUserResponse> getUserById(@PathVariable UUID userId) {
        InternalUserResponse response = authService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/auth/register ──────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Registration successful. "
                                + "Please check your email for the OTP."
                ));
    }


    // ── POST /api/auth/resend-otp ────────────────────────────────

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        authService.resendOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                "message", "OTP resent successfully. "
                        + "Please check your email."
        ));
    }


    // ── POST /api/auth/verify-email ──────────────────────────

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @Valid @RequestBody VerifyOtpRequest request) {

        authService.verifyEmail(request);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully. You can now log in."
        ));
    }

    // ── POST /api/auth/login ─────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/auth/refresh ───────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody Map<String, String> body) {

        String refreshToken = body.get("refresh_token");

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    // ── POST /api/auth/logout ────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader("Authorization") String authorizationHeader) {

        String token = authorizationHeader.substring("Bearer ".length());
        String email = extractEmailFromHeader(authorizationHeader);

        authService.logout(token, email);

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully."
        ));
    }

    // ── POST /api/auth/forgot-password ───────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody Map<String, String> body) {

        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email is required."));
        }

        authService.forgotPassword(email);

        // Always return the same response — never reveal
        // whether the email exists in the system
        return ResponseEntity.ok(Map.of(
                "message", "If this email is registered, "
                        + "you will receive a reset OTP shortly."
        ));
    }

    // ── POST /api/auth/reset-password ────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Password reset successful. Please log in with your new password."
        ));
    }


    // ── GET /api/auth/users/internal/all ────────────────────
// Internal — called by admin-service only
// Returns paginated list of all users

    @GetMapping("/users/internal/all")
    public ResponseEntity<Page<InternalUserResponse>> getAllUsers(
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {

        return ResponseEntity.ok(authService.getAllUsers(pageable));
    }

// ── PUT /api/auth/users/internal/{userId}/ban ────────────
// Internal — admin bans a user (sets isActive = false)

    @PutMapping("/users/internal/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID userId) {
        authService.banUser(userId);
        return ResponseEntity.ok().build();
    }

// ── PUT /api/auth/users/internal/{userId}/unban ──────────
// Internal — admin unbans a user (sets isActive = true)

    @PutMapping("/users/internal/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
        authService.unbanUser(userId);
        return ResponseEntity.ok().build();
    }

    // ── Private Helper ───────────────────────────────────────

    private String extractEmailFromHeader(String authorizationHeader) {
        // This is a lightweight extract — JwtService is not injected
        // into the controller. The filter has already validated the
        // token before this method is reached, so the token is trusted.
        // AuthServiceImpl re-extracts email internally via JwtService.
        String token = authorizationHeader.substring("Bearer ".length());
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed JWT in Authorization header.");
        }

        java.util.Base64.Decoder decoder = java.util.Base64.getUrlDecoder();
        String payload = new String(decoder.decode(parts[1]));

        // Parse "sub" claim from payload JSON manually —
        // avoids injecting JwtService into the controller layer
        int subIndex = payload.indexOf("\"sub\":\"");
        if (subIndex == -1) {
            throw new IllegalArgumentException("Subject claim missing from JWT payload.");
        }

        int start = subIndex + 7;
        int end   = payload.indexOf("\"", start);
        return payload.substring(start, end);
    }
}