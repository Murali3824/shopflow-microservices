package com.shopflow.seller.exception;

public class UnauthorizedCouponAccessException extends RuntimeException {
    public UnauthorizedCouponAccessException(String message) {
        super(message);
    }
}