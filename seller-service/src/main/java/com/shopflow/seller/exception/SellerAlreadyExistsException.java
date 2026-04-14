package com.shopflow.seller.exception;

public class SellerAlreadyExistsException extends RuntimeException {
    public SellerAlreadyExistsException(String message) {
        super(message);
    }
}