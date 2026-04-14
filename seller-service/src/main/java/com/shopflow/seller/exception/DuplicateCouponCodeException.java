package com.shopflow.seller.exception;

public class DuplicateCouponCodeException extends RuntimeException {
    public DuplicateCouponCodeException(String message) {
        super(message);
    }
}