package com.shopflow.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Centralized error handler for the API Gateway (WebFlux).
 *
 * <p>Catches exceptions that bubble up through the gateway pipeline and returns
 * a consistent JSON error envelope instead of the default Netty HTML error page.
 *
 * <p>All responses have the shape:
 * <pre>
 * {
 *   "timestamp": "2025-01-01T00:00:00Z",
 *   "status":    401,
 *   "error":     "Unauthorized",
 *   "message":   "Human-readable explanation"
 * }
 * </pre>
 */
@Slf4j
@Order(-1)          // run before Spring Boot's DefaultErrorWebExceptionHandler
@Component
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // Already committed — nothing we can do
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status;
        String      message;

        if (ex instanceof ResponseStatusException rse) {
            status  = HttpStatus.resolve(rse.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();

        } else if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
            // No route found for the request
            status  = HttpStatus.NOT_FOUND;
            message = "No route found — check the request path";

        } else {
            status  = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "An unexpected error occurred";
            log.error("Unhandled gateway exception: {}", ex.getMessage(), ex);
        }

        log.warn("Gateway error — status={}, message={}, path={}",
                status.value(), message, exchange.getRequest().getPath());

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = buildBody(status, message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildBody(HttpStatus status, String message) {
        // Manual JSON — no Jackson dependency needed in gateway
        return """
                {
                  "timestamp": "%s",
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }""".formatted(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                escapeJson(message)
        );
    }

    /** Minimal JSON string escaping for the message field. */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}