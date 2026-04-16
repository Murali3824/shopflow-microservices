package com.shopflow.product.exception;

public class UnauthorizedProductAccessException extends RuntimeException {
    public UnauthorizedProductAccessException(String message) {
        super(message);
    }
}