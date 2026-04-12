package com.shopflow.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String OTP_PREFIX       = "otp:";
    private static final String OTP_RESET_PREFIX = "otp:reset:";
    private static final int    OTP_LENGTH        = 6;

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${application.otp.expiry-minutes}")
    private long otpExpiryMinutes;

    // ── Generate OTP ─────────────────────────────────────────

    public String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = random.nextInt(900_000) + 100_000;   // always 6 digits: 100000–999999
        return String.valueOf(otp);
    }

    // ── Save OTP to Redis ────────────────────────────────────

    public void saveOtpToRedis(String email, String otp) {
        String key = buildKey(OTP_PREFIX, email);
        redisTemplate.opsForValue().set(key, otp, otpExpiryMinutes, TimeUnit.MINUTES);
        log.debug("OTP saved to Redis for email: {} | TTL: {} minutes", email, otpExpiryMinutes);
    }

    public void saveResetOtpToRedis(String email, String otp) {
        String key = buildKey(OTP_RESET_PREFIX, email);
        redisTemplate.opsForValue().set(key, otp, otpExpiryMinutes, TimeUnit.MINUTES);
        log.debug("Reset OTP saved to Redis for email: {} | TTL: {} minutes", email, otpExpiryMinutes);
    }

    // ── Validate OTP ─────────────────────────────────────────

    public boolean validateOtp(String email, String otp) {
        String key = buildKey(OTP_PREFIX, email);
        return checkOtp(key, otp);
    }

    public boolean validateResetOtp(String email, String otp) {
        String key = buildKey(OTP_RESET_PREFIX, email);
        return checkOtp(key, otp);
    }

    // ── Delete OTP ───────────────────────────────────────────

    public void deleteOtp(String email) {
        String key = buildKey(OTP_PREFIX, email);
        redisTemplate.delete(key);
        log.debug("OTP deleted from Redis for email: {}", email);
    }

    public void deleteResetOtp(String email) {
        String key = buildKey(OTP_RESET_PREFIX, email);
        redisTemplate.delete(key);
        log.debug("Reset OTP deleted from Redis for email: {}", email);
    }

    // ── Private Helpers ──────────────────────────────────────

    private String buildKey(String prefix, String email) {
        return prefix + email;
    }

    private boolean checkOtp(String key, String otp) {
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            log.warn("OTP not found or expired for key: {}", key);
            return false;
        }

        boolean matches = stored.equals(otp);

        if (!matches) {
            log.warn("OTP mismatch for key: {}", key);
        }

        return matches;
    }
}