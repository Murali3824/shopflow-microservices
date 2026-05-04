package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private Boolean isActive;
    private Boolean isVerified;
}