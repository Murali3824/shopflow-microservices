package com.shopflow.product.client;

public record SellerValidationResponse(

        java.util.UUID id,
        String status

) {
    // Called by product-service before creating a product
    // to confirm the seller is APPROVED, not PENDING or REJECTED
    public boolean isApproved() {
        return "APPROVED".equals(status);
    }
}