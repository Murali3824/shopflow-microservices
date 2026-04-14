package com.shopflow.seller.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String message) {
        super(message);
    }
}