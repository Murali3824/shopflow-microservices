package com.shopflow.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderRequest(

        @NotNull(message = "Address ID is required")
        UUID addressId,

        @NotBlank(message = "Payment method is required")
        String paymentMethod,

        String couponCode
) {}