package com.shopflow.seller.service;

import com.shopflow.seller.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SellerService {

    SellerResponse registerSeller(UUID userId, SellerRegistrationRequest request);

    SellerResponse getSellerProfile(UUID userId);

    SellerResponse updateStore(UUID userId, StoreUpdateRequest request);

    BigDecimal getEarnings(UUID userId);

    CouponResponse createCoupon(UUID userId, CouponRequest request);

    List<CouponResponse> getCoupons(UUID userId);

    void deleteCoupon(UUID userId, UUID couponId);

    CouponValidationResponse validateCoupon(String code, BigDecimal orderTotal);

    SellerInternalResponse getSellerInternal(UUID sellerId);

    Page<SellerResponse> getAllSellers(Pageable pageable);
    List<SellerResponse> getPendingSellers();
    void approveSeller(UUID sellerId);
    void rejectSeller(UUID sellerId);
    void updateCommission(UUID sellerId, BigDecimal commissionRate);

}