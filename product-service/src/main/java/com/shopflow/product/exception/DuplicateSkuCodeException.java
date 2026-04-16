package com.shopflow.product.exception;

public class DuplicateSkuCodeException extends RuntimeException {
    public DuplicateSkuCodeException(String message) {
        super(message);
    }
}