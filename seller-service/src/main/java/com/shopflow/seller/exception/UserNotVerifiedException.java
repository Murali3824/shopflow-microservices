package com.shopflow.seller.exception;

public class UserNotVerifiedException extends RuntimeException {
    public UserNotVerifiedException(String message) {
        super(message);
    }
}