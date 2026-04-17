package com.shopflow.order.exception;

public class ReturnRequestException extends RuntimeException {
    public ReturnRequestException(String message) {
        super(message);
    }
}