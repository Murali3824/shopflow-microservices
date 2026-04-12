package com.shopflow.auth.security;

import com.shopflow.auth.entity.roles.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(
            @Value("${application.jwt.secret}") String secret,
            @Value("${application.jwt.access-token-expiry-ms}") long accessTokenExpiryMs,
            @Value("${application.jwt.refresh-token-expiry-ms}") long refreshTokenExpiryMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }

    // ── Generate Access Token ────────────────────────────────

    public String generateAccessToken(String email, Role role,UUID userId) {
        return buildToken(email, role,userId, accessTokenExpiryMs);
    }

    // ── Generate Refresh Token ───────────────────────────────

    public String generateRefreshToken(String email, Role role,UUID userId) {
        return buildToken(email, role,userId, refreshTokenExpiryMs);
    }

    // ── Validate Token ───────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
            return false;
        }
    }

    // ── Extract Email ────────────────────────────────────────

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // ── Extract Role ─────────────────────────────────────────

    public Role extractRole(String token) {
        String roleValue = parseClaims(token).get("role", String.class);
        return Role.valueOf(roleValue);
    }

    // ── Private Helpers ──────────────────────────────────────
    private String buildToken(String email, Role role,
                              UUID userId, long expiryMs) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .claim("userId", userId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public long getRemainingExpiryMs(String token) {
        Date expiration = parseClaims(token).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    public UUID extractUserId(String token) {
        String userId = parseClaims(token).get("userId", String.class);
        return UUID.fromString(userId);
    }
}