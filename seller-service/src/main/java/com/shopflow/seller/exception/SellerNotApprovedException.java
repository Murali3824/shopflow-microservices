package com.shopflow.seller.exception;

public class SellerNotApprovedException extends RuntimeException {
    public SellerNotApprovedException(String message) {
        super(message);
    }
}