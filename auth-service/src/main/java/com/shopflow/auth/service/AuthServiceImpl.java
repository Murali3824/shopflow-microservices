package com.shopflow.auth.service;

import com.shopflow.auth.dto.*;
import com.shopflow.auth.entity.RefreshToken;
import com.shopflow.auth.entity.User;
import com.shopflow.auth.event.OtpRequestedEvent;
import com.shopflow.auth.event.UserEventProducer;
import com.shopflow.auth.event.UserRegisteredEvent;
import com.shopflow.auth.exception.EmailAlreadyExistsException;
import com.shopflow.auth.exception.InvalidOtpException;
import com.shopflow.auth.exception.InvalidTokenException;
import com.shopflow.auth.exception.ResourceNotFoundException;
import com.shopflow.auth.publisher.KafkaEventPublisher;
import com.shopflow.auth.repository.RefreshTokenRepository;
import com.shopflow.auth.repository.UserRepository;
import com.shopflow.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService             jwtService;
    private final OtpService             otpService;
    private final KafkaEventPublisher    kafkaEventPublisher;
    private final PasswordEncoder        passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserEventProducer userEventProducer;

    @Value("${application.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${application.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;


    @Override
    @Transactional(readOnly = true)
    public InternalUserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return InternalUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .build();
    }

    // ── Register ─────────────────────────────────────────────
    @Override
    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .isVerified(false)
                .isActive(true)
                .build();

        userRepository.save(user);

        String otp = otpService.generateOtp();
        otpService.saveOtpToRedis(request.getEmail(), otp);

        kafkaEventPublisher.publishOtpRequested(
                OtpRequestedEvent.builder()
                        .email(request.getEmail())
                        .fullName(request.getFullName())
                        .otp(otp)
                        .otpType(OtpRequestedEvent.OtpType.REGISTRATION)
                        .build()
        );

        log.info("User registered, OTP sent | email: {}", request.getEmail());
    }


    @Override
    public void resendOtp(String email) {

        User user = findActiveUserByEmail(email);

        if (user.isVerified()) {
            throw new ResourceNotFoundException(
                    "Account already verified for: " + email);
        }

        // Delete existing OTP if one is still alive in Redis
        otpService.deleteOtp(email);

        String otp = otpService.generateOtp();
        otpService.saveOtpToRedis(email, otp);

        kafkaEventPublisher.publishOtpRequested(
                OtpRequestedEvent.builder()
                        .email(email)
                        .fullName(user.getFullName())
                        .otp(otp)
                        .otpType(OtpRequestedEvent.OtpType.RESEND)
                        .build()
        );

        log.info("OTP resent | email: {}", email);
    }


    // ── Verify Email ─────────────────────────────────────────
    @Override
    @Transactional
    public void verifyEmail(VerifyOtpRequest request) {

        User user = findActiveUserByEmail(request.getEmail());

        if (user.isVerified()) {
            throw new ResourceNotFoundException("Account already verified for: " + request.getEmail());
        }

        if (!otpService.validateOtp(request.getEmail(), request.getOtp())) {
            throw new InvalidOtpException("Invalid or expired OTP for: " + request.getEmail());
        }

        user.setVerified(true);
        userRepository.save(user);

        otpService.deleteOtp(request.getEmail());

        // publish event AFTER verification confirmed
        // user-service creates profile only for verified users
        userEventProducer.publishUserRegistered(
                UserRegisteredEvent.builder()
                        .userId(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .build()
        );

        log.info("Email verified successfully | email: {}", request.getEmail());
    }

    // ── Login ────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        User user = findActiveUserByEmail(request.getEmail());

        if (!user.isVerified()) {
            throw new InvalidTokenException("Account not verified. Check your email for the OTP.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidTokenException("Invalid credentials.");
        }

        String accessToken  = jwtService.generateAccessToken(
                user.getEmail(), user.getRole(), user.getId());
        String refreshToken = jwtService.generateRefreshToken(
                user.getEmail(), user.getRole(), user.getId());

        saveRefreshToken(user, refreshToken);

        log.info("User logged in | email: {}", user.getEmail());

        return buildAuthResponse(accessToken, refreshToken, user.getId());
    }

    // ── Refresh Token ────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {

        if (!jwtService.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid or expired refresh token.");
        }

        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found."));

        if (stored.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked.");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired.");
        }

        User user = stored.getUser();

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String newAccessToken  = jwtService.generateAccessToken(
                user.getEmail(), user.getRole(), user.getId());
        String newRefreshToken = jwtService.generateRefreshToken(
                user.getEmail(), user.getRole(), user.getId());

        saveRefreshToken(user, newRefreshToken);

        log.info("Tokens refreshed | email: {}", user.getEmail());

        return buildAuthResponse(newAccessToken, newRefreshToken, user.getId());
    }

    // ── Logout ───────────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String accessToken, String email) {

        long remainingTtl = jwtService.getRemainingExpiryMs(accessToken);

        if (remainingTtl > 0) {
            String blacklistKey = BLACKLIST_PREFIX + accessToken;
            redisTemplate.opsForValue().set(
                    blacklistKey, "1", remainingTtl, TimeUnit.MILLISECONDS
            );
        }

        refreshTokenRepository.deleteByUserId(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email))
                        .getId()
        );

        log.info("User logged out | email: {}", email);
    }

    // ── Forgot Password ──────────────────────────────────────

    @Override
    public void forgotPassword(String email) {

        User user = findActiveUserByEmail(email);

        if (!user.isVerified()) {
            throw new InvalidTokenException   ("Account not verified for: " + email);
        }

        String otp = otpService.generateOtp();
        otpService.saveResetOtpToRedis(email, otp);

        kafkaEventPublisher.publishOtpRequested(
                OtpRequestedEvent.builder()
                        .email(email)
                        .fullName(user.getFullName())
                        .otp(otp)
                        .otpType(OtpRequestedEvent.OtpType.PASSWORD_RESET)
                        .build()
        );

        log.info("Password reset OTP sent | email: {}", email);
    }

    // ── Reset Password ───────────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {

        User user = findActiveUserByEmail(email);

        if (!otpService.validateResetOtp(email, otp)) {
            throw new InvalidOtpException("Invalid or expired reset OTP for: " + email);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpService.deleteResetOtp(email);

        refreshTokenRepository.deleteByUserId(user.getId());

        log.info("Password reset successful | email: {}", email);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<InternalUserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(user -> InternalUserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().name())
                        .isVerified(user.isVerified())
                        .isActive(user.isActive())
                        .build());
    }

    @Override
    @Transactional
    public void banUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        user.setActive(false);
        userRepository.save(user);
        log.info("User banned by admin | userId: {}", userId);
    }

    @Override
    @Transactional
    public void unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        user.setActive(true);
        userRepository.save(user);
        log.info("User unbanned by admin | userId: {}", userId);
    }

    // ── Private Helpers ──────────────────────────────────────

    private User findActiveUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User not found or inactive: " + email));
    }

    private void saveRefreshToken(User user, String rawToken) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(rawToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiryMs / 1000))
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(String accessToken,
                                           String refreshToken,
                                           UUID userId) {
        return AuthResponse.builder()
                .userId(userId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiryMs)
                .build();
    }
}