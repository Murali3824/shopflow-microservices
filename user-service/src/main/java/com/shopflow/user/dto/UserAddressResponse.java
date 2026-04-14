package com.shopflow.user.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressResponse {

    private UUID id;
    private UUID userId;
    private String label;
    private String street;
    private String city;
    private String state;
    private String pincode;
    private boolean isDefault;
}