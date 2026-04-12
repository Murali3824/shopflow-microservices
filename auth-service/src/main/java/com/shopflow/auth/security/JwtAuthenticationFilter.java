package com.shopflow.auth.security;

import com.shopflow.auth.entity.roles.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String BLACKLIST_PREFIX     = "blacklist:";

    private final JwtService                    jwtService;
    private final RedisTemplate<String, String> redisTemplate;

    // ── Filter Logic ─────────────────────────────────────────

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isBlacklisted(token)) {
            log.warn("Blacklisted token used | path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateToken(token)) {
            log.warn("Invalid JWT token | path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            setAuthentication(token, request);
        }

        filterChain.doFilter(request, response);
    }

    // ── Skip Filter for Public Endpoints ─────────────────────

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/register")       ||
                path.startsWith("/auth/verify-email")   ||
                path.startsWith("/auth/login")          ||
                path.startsWith("/auth/forgot-password")||
                path.startsWith("/auth/reset-password") ||
                path.startsWith("/auth/resend-otp")     ||
                path.startsWith("/actuator");
    }

    // ── Private Helpers ──────────────────────────────────────

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        return header.substring(BEARER_PREFIX.length());
    }

    private boolean isBlacklisted(String token) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    private void setAuthentication(String token, HttpServletRequest request) {
        try {
            String email = jwtService.extractEmail(token);
            Role   role  = jwtService.extractRole(token);

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role.name());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(authority)
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Authentication set | email: {} | role: {}", email, role);

        } catch (Exception e) {
            log.error("Failed to set authentication from token: {}", e.getMessage());
        }
    }
}