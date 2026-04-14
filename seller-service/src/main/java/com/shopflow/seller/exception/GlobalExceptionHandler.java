package com.shopflow.seller.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp
    ) {}


    // ─── Seller Exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(SellerNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleSellerNotApproved(SellerNotApprovedException ex) {
        return build(HttpStatus.FORBIDDEN, "Seller Not Approved", ex.getMessage());
    }

    @ExceptionHandler(SellerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSellerAlreadyExists(SellerAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "Seller Already Exists", ex.getMessage());
    }

    @ExceptionHandler(SellerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSellerNotFound(SellerNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Seller Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateGstException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateGst(DuplicateGstException ex) {
        return build(HttpStatus.CONFLICT, "Duplicate GST Number", ex.getMessage());
    }

    // ─── Coupon Exceptions ────────────────────────────────────────────────────

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCouponNotFound(CouponNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Coupon Not Found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateCouponCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateCouponCode(DuplicateCouponCodeException ex) {
        return build(HttpStatus.CONFLICT, "Duplicate Coupon Code", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedCouponAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedCouponAccess(UnauthorizedCouponAccessException ex) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    // ─── User Exceptions (from auth-service Feign calls) ─────────────────────

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "User Not Found", ex.getMessage());
    }

    @ExceptionHandler(UserNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleUserNotVerified(UserNotVerifiedException ex) {
        return build(HttpStatus.FORBIDDEN, "User Not Verified", ex.getMessage());
    }

    @ExceptionHandler(UserNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleUserNotActive(UserNotActiveException ex) {
        return build(HttpStatus.FORBIDDEN, "User Not Active", ex.getMessage());
    }

    // ─── Generic Resource Not Found ───────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    // ─── Validation Exceptions ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return build(HttpStatus.BAD_REQUEST, "Validation Failed", message);
    }

    // ─── Feign Exceptions ─────────────────────────────────────────────────────

    @ExceptionHandler(FeignException.ServiceUnavailable.class)
    public ResponseEntity<ErrorResponse> handleFeignServiceUnavailable(FeignException.ServiceUnavailable ex) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Auth Service Unavailable",
                "Cannot reach auth-service. Please try again later.");
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        return build(HttpStatus.BAD_GATEWAY, "Upstream Service Error",
                "An error occurred communicating with an upstream service.");
    }

    // ─── Fallback ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred.");
    }

    // ─── Builder ──────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity
                .status(status)
                .body(new ErrorResponse(
                        status.value(),
                        error,
                        message,
                        LocalDateTime.now()
                ));
    }
}