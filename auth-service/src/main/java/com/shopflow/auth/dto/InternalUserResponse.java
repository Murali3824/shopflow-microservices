package com.shopflow.auth.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String role;
    private Boolean isVerified;
    private Boolean isActive;
}