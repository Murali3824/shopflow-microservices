package com.shopflow.auth.service;

import com.shopflow.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AuthService {

    InternalUserResponse getUserById(UUID userId);

    void register(RegisterRequest request);

    void verifyEmail(VerifyOtpRequest request);

    void resendOtp(String email);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(String refreshToken);

    void logout(String accessToken, String email);

    void forgotPassword(String email);

    void resetPassword(String email, String otp, String newPassword);

    Page<InternalUserResponse> getAllUsers(Pageable pageable);
    void banUser(UUID userId);
    void unbanUser(UUID userId);
}