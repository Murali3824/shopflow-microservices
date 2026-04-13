package com.shopflow.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Global JWT filter for Spring Cloud Gateway (WebFlux).
 *
 * <p>Runs on every request. Public endpoints are whitelisted and passed through
 * immediately. All other requests must carry a valid Bearer token.
 *
 * <p>On success, two downstream headers are injected:
 * <ul>
 *   <li>{@code X-Auth-User-Email} — the subject claim from the token</li>
 *   <li>{@code X-Auth-User-Role}  — the "role" claim from the token</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    // ── Whitelist ────────────────────────────────────────────────────────────
    // Pairs of (HttpMethod, path-pattern). Use null method to allow any method.
    private static final List<RouteEntry> PUBLIC_ROUTES = List.of(
            new RouteEntry(HttpMethod.POST,  "/api/auth/register"),
            new RouteEntry(HttpMethod.POST,  "/api/auth/login"),
            new RouteEntry(HttpMethod.POST,  "/api/auth/verify-email"),
            new RouteEntry(HttpMethod.POST,  "/api/auth/forgot-password"),
            new RouteEntry(HttpMethod.POST,  "/api/auth/reset-password"),
            // Razorpay / Stripe webhooks must be unauthenticated
            new RouteEntry(HttpMethod.POST,  "/api/payments/webhook/**"),
            // Public read access to product catalog
            new RouteEntry(HttpMethod.GET,   "/api/products/**"),
            new RouteEntry(HttpMethod.GET,   "/api/categories/**"),
            new RouteEntry(HttpMethod.GET,   "/api/reviews/product/**")
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Value("${jwt.secret}")
    private String jwtSecret;

    // ── GlobalFilter ─────────────────────────────────────────────────────────

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicRoute(request)) {
            log.debug("Public route — skipping JWT check: {} {}", request.getMethod(), request.getPath());
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or malformed Authorization header for: {} {}", request.getMethod(), request.getPath());
            return unauthorized(exchange, "Authorization header missing or malformed");
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseClaims(token);

            String email  = claims.getSubject();
            String role   = claims.get("role", String.class);
            String userId = claims.get("userId", String.class);

            log.debug("JWT valid — userId={}, email={}, role={}, path={}", userId, email, role, request.getPath());

            // Mutate request: add downstream identity headers
            // We standardize on X-User-Id, X-User-Email, X-User-Role
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id",    userId != null ? userId : "")
                    .header("X-User-Email", email  != null ? email  : "")
                    .header("X-User-Role",  role   != null ? role   : "")
                    // Providing fallback for older header names to ensure backward compatibility
                    .header("X-Auth-User-Email", email != null ? email : "")
                    .header("X-Auth-User-Role",  role  != null ? role  : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
            return unauthorized(exchange, "Token has expired");

        } catch (SignatureException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
            return unauthorized(exchange, "Token signature is invalid");

        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("JWT malformed: {}", e.getMessage());
            return unauthorized(exchange, "Token is malformed");
        }
    }

    /** Run before routing (highest precedence). */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isPublicRoute(ServerHttpRequest request) {
        String path   = request.getPath().value();
        HttpMethod method = request.getMethod();

        return PUBLIC_ROUTES.stream().anyMatch(entry -> {
            boolean methodMatch = entry.method() == null || entry.method().equals(method);
            boolean pathMatch   = PATH_MATCHER.match(entry.pattern(), path);
            return methodMatch && pathMatch;
        });
    }

    private Claims parseClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"status":401,"error":"Unauthorized","message":"%s"}
                """.formatted(message).strip();

        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    // ── Inner record ─────────────────────────────────────────────────────────

    private record RouteEntry(HttpMethod method, String pattern) {}
}