package com.shopflow.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequestedEvent {

    private String email;
    private String fullName;
    private String otp;
    private OtpType otpType;

    public enum OtpType {
        REGISTRATION,
        RESEND,
        PASSWORD_RESET
    }
}