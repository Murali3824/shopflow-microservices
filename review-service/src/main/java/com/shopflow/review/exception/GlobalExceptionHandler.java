package com.shopflow.review.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────────────────────
    // 404 — Review Not Found
    // ─────────────────────────────────────────────

    @ExceptionHandler(ReviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReviewNotFound(ReviewNotFoundException ex) {
        log.warn("Review not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    // ─────────────────────────────────────────────
    // 409 — Duplicate Review
    // ─────────────────────────────────────────────

    @ExceptionHandler(DuplicateReviewException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateReview(DuplicateReviewException ex) {
        log.warn("Duplicate review attempt: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    // ─────────────────────────────────────────────
    // 403 — Unauthorized Access
    // ─────────────────────────────────────────────

    @ExceptionHandler(UnauthorizedReviewAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedReviewAccessException ex) {
        log.warn("Unauthorized review access: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // ─────────────────────────────────────────────
    // 422 — Purchase Verification Failed
    // ─────────────────────────────────────────────

    @ExceptionHandler(PurchaseVerificationException.class)
    public ResponseEntity<ErrorResponse> handlePurchaseVerification(
            PurchaseVerificationException ex) {
        log.warn("Purchase verification failed: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
    }

    // ─────────────────────────────────────────────
    // 400 — Validation Errors
    // ─────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", message);
    }

    // ─────────────────────────────────────────────
    // 503 — Feign / Order Service Unreachable
    // ─────────────────────────────────────────────

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        log.error("Feign call failed — status={} message={}", ex.status(), ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "Order service is currently unavailable. Please try again later.");
    }

    // ─────────────────────────────────────────────
    // 500 — Fallback
    // ─────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred");
    }

    // ─────────────────────────────────────────────
    // Builder Helper
    // ─────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}