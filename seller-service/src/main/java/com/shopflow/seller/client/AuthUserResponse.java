package com.shopflow.seller.client;


import java.util.UUID;


public record AuthUserResponse(
        UUID id,
        String email,
        String fullName,
        String role,
        Boolean isVerified,
        Boolean isActive
) {}