// InvalidOtpException.java
package com.shopflow.auth.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String email) {
        super("Invalid or expired OTP for: " + email);
    }
}