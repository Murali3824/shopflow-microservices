package com.shopflow.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.IllegalQueryOperationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;


// ── GatewayHeaderFilter.java ─────────────────────────────────
// API Gateway validates the JWT and forwards userId + role
// as plain headers. This service just reads those headers —
// it never parses or validates JWT itself.
@Slf4j
@Component
class GatewayHeaderFilter extends OncePerRequestFilter {

    // these header names must match exactly what api-gateway sends
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String roleHeader   = request.getHeader(HEADER_USER_ROLE);

        // if gateway did not forward the userId header,
        // it's either a public route or unauthenticated.
        // We skip setting authentication and let SecurityConfig handle it.
        if (userIdHeader == null || userIdHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);
            String role = (roleHeader != null) ? roleHeader : "USER";

            // build Spring Security authentication from headers
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,                                      // principal — used in controller
                            null,                                        // credentials — not needed
                            List.of(new SimpleGrantedAuthority(role))   // authorities
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated userId: {} role: {}", userId, role);

        } catch (IllegalArgumentException e) {
            // X-User-Id header exists but is not a valid UUID
            log.warn("Invalid UUID in {} header: {}", HEADER_USER_ID, userIdHeader);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"status":401,"error":"Unauthorized","message":"Invalid authentication headers"}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}