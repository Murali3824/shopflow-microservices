package com.shopflow.seller.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record StoreUpdateRequest(

        @Size(max = 255, message = "Store name must not exceed 255 characters")
        String storeName,

        String storeDescription,

        String logoUrl
) {}