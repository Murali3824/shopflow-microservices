package com.shopflow.seller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SellerRegistrationRequest(

        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name must not exceed 255 characters")
        String businessName,

        @NotBlank(message = "GST number is required")
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                message = "Invalid GST number format")
        String gstNumber,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Invalid phone number format")
        String phone
) {}