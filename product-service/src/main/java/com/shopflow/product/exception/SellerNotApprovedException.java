package com.shopflow.product.exception;

public class SellerNotApprovedException extends RuntimeException {
    public SellerNotApprovedException(String message) {
        super(message);
    }
}