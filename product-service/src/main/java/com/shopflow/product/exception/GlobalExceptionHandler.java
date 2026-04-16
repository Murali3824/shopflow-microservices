package com.shopflow.product.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // 404 — Resource not found
    // Catches both ProductNotFoundException and CategoryNotFoundException
    // since both extend ResourceNotFoundException.
    // ------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        404,
                        "Not Found",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 403 — Seller not approved to list products
    // ------------------------------------------------------------------

    @ExceptionHandler(SellerNotApprovedException.class)
    public ResponseEntity<ErrorResponse> handleSellerNotApproved(
            SellerNotApprovedException ex,
            HttpServletRequest request) {

        log.warn("Seller not approved: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        403,
                        "Forbidden",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 403 — Seller attempting to modify another seller's product
    // ------------------------------------------------------------------

    @ExceptionHandler(UnauthorizedProductAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
            UnauthorizedProductAccessException ex,
            HttpServletRequest request) {

        log.warn("Unauthorized product access: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        403,
                        "Forbidden",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 409 — Stock reservation failed — not enough units available
    // ------------------------------------------------------------------

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex,
            HttpServletRequest request) {

        log.warn("Insufficient stock: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        409,
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 409 — Duplicate SKU code on create or update
    // Handles both explicit DuplicateSkuCodeException thrown by the
    // service layer and DataIntegrityViolationException thrown by
    // Hibernate when the DB unique constraint fires.
    // ------------------------------------------------------------------

    @ExceptionHandler(DuplicateSkuCodeException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateSkuCode(
            DuplicateSkuCodeException ex,
            HttpServletRequest request) {

        log.warn("Duplicate SKU code: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        409,
                        "Conflict",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Data integrity violation: {}", ex.getMessage());

        // Inspect the root cause message to give a specific error
        // rather than a generic database error to the client
        String cause = ex.getRootCause() != null
                ? ex.getRootCause().getMessage()
                : "";

        String clientMessage = cause.contains("uq_product_skus_sku_code")
                ? "A SKU with this code already exists"
                : "A duplicate value violates a unique constraint";

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        409,
                        "Conflict",
                        clientMessage,
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 400 — Jakarta Validation failures (@Valid on request body)
    // Returns field-level details so the client knows exactly
    // which fields failed and why.
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<ErrorResponse.FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldViolation(
                        error.getField(),
                        error.getDefaultMessage()
                ))
                .toList();

        log.warn("Validation failed on {}: {} violation(s)",
                request.getRequestURI(), violations.size());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.ofValidation(
                        400,
                        "Bad Request",
                        "Validation failed",
                        request.getRequestURI(),
                        violations
                ));
    }

    // ------------------------------------------------------------------
    // 503 — Feign call to seller-service failed
    // Seller-service being down must not produce a 500 — it is a
    // known dependency failure with a meaningful message for the caller.
    // ------------------------------------------------------------------

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex,
            HttpServletRequest request) {

        log.error("Feign call failed — status: {} message: {}",
                ex.status(), ex.getMessage());

        // If seller-service returned a 404 the seller does not exist
        if (ex.status() == 404) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(
                            400,
                            "Bad Request",
                            "Seller not found",
                            request.getRequestURI()
                    ));
        }

        // Any other Feign failure is a dependency availability problem
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(
                        503,
                        "Service Unavailable",
                        "A dependent service is currently unavailable. Please try again.",
                        request.getRequestURI()
                ));
    }

    // ------------------------------------------------------------------
    // 500 — Catch-all for anything not handled above
    // Never expose internal details to the client.
    // ------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "Internal Server Error",
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI()
                ));
    }


    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(
            FileStorageException ex,
            HttpServletRequest request) {

        log.error("File storage error: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        500,
                        "Internal Server Error",
                        "File storage operation failed. Please try again.",
                        request.getRequestURI()
                ));
    }

}