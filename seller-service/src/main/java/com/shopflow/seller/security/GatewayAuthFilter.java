package com.shopflow.seller.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip authentication for internal service-to-service endpoints.
        // These are called by other microservices via Feign, not by users.
        // They carry no user identity headers — that is expected and correct.
        if (path.startsWith("/api/sellers/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String userRoleHeader = request.getHeader(HEADER_USER_ROLE);

        // if gateway did not forward the userId header,
        // it's either an internal service call or unauthenticated.
        // We skip setting authentication and let SecurityConfig handle it.
        if (userIdHeader == null || userRoleHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID userId = UUID.fromString(userIdHeader);

            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + userRoleHeader.toUpperCase());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(authority)
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "status": 400,
                    "error": "Bad Request",
                    "message": "Invalid user ID format in request header"
                }
                """);
        }
    }
}