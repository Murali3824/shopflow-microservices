package com.shopflow.product.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(

        int status,
        String error,
        String message,
        String path,

        // Only present on validation errors — null otherwise.
        // @JsonInclude above ensures it is omitted from JSON when null.
        List<FieldViolation> violations,

        LocalDateTime timestamp

) {
    // Convenience factory for single-message errors
    public static ErrorResponse of(int status, String error,
                                   String message, String path) {
        return new ErrorResponse(
                status, error, message, path, null, LocalDateTime.now()
        );
    }

    // Convenience factory for validation errors with field details
    public static ErrorResponse ofValidation(int status, String error,
                                             String message, String path,
                                             List<FieldViolation> violations) {
        return new ErrorResponse(
                status, error, message, path, violations, LocalDateTime.now()
        );
    }

    public record FieldViolation(String field, String message) {}
}