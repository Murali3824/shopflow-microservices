package com.shopflow.payment.exception;

public class InvalidGatewayException extends RuntimeException {
    public InvalidGatewayException(String message) {
        super(message);
    }
}