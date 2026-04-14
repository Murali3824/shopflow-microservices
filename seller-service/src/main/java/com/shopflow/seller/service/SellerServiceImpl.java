package com.shopflow.seller.service;

import com.shopflow.seller.client.AuthServiceClient;
import com.shopflow.seller.dto.*;
import com.shopflow.seller.entity.Coupon;
import com.shopflow.seller.entity.Seller;
import com.shopflow.seller.entity.status.SellerStatus;
import com.shopflow.seller.entity.type.DiscountType;
import com.shopflow.seller.exception.*;
import com.shopflow.seller.repository.CouponRepository;
import com.shopflow.seller.repository.SellerRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final CouponRepository couponRepository;
    private final AuthServiceClient authServiceClient;

    // ─── Approval Guard ───────────────────────────────────────────────────────

    private void validateApproved(Seller seller) {
        if (seller.getStatus() != SellerStatus.APPROVED) {
            throw new SellerNotApprovedException(
                    "Your seller account is pending admin approval"
            );
        }
    }

    // ─── Register ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SellerResponse registerSeller(UUID userId, SellerRegistrationRequest request) {

        try {
            var authUser = authServiceClient.getUserById(userId);

            if (Boolean.FALSE.equals(authUser.isVerified())) {
                throw new UserNotVerifiedException("User email is not verified");
            }

            if (Boolean.FALSE.equals(authUser.isActive())) {
                throw new UserNotActiveException("User account is not active");
            }

        } catch (FeignException.NotFound e) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        if (sellerRepository.existsByUserId(userId)) {
            throw new SellerAlreadyExistsException("Seller account already exists for this user");
        }

        if (sellerRepository.existsByGstNumber(request.gstNumber())) {
            throw new DuplicateGstException("GST number already registered: " + request.gstNumber());
        }

        Seller seller = Seller.builder()
                .userId(userId)
                .businessName(request.businessName())
                .gstNumber(request.gstNumber())
                .phone(request.phone())
                .build();

        Seller saved = sellerRepository.save(seller);
        return toSellerResponse(saved);
    }

    // ─── Profile ──────────────────────────────────────────────────────────────

    // No validateApproved here — PENDING sellers must be able to check their status
    @Override
    @Transactional(readOnly = true)
    public SellerResponse getSellerProfile(UUID userId) {
        Seller seller = findSellerByUserId(userId);
        return toSellerResponse(seller);
    }

    // ─── Store Update ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SellerResponse updateStore(UUID userId, StoreUpdateRequest request) {
        Seller seller = findSellerByUserId(userId);
        validateApproved(seller);

        if (request.storeName() != null) {
            seller.setStoreName(request.storeName());
        }
        if (request.storeDescription() != null) {
            seller.setStoreDescription(request.storeDescription());
        }
        if (request.logoUrl() != null) {
            seller.setLogoUrl(request.logoUrl());
        }

        Seller updated = sellerRepository.save(seller);
        return toSellerResponse(updated);
    }

    // ─── Earnings ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getEarnings(UUID userId) {
        Seller seller = findSellerByUserId(userId);
        validateApproved(seller);

        // Earnings live in payment-service SELLER_EARNING table.
        // Replaced with Feign call to payment-service once that service is built.
        return BigDecimal.ZERO;
    }

    // ─── Coupons ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CouponResponse createCoupon(UUID userId, CouponRequest request) {
        Seller seller = findSellerByUserId(userId);
        validateApproved(seller);

        if (couponRepository.existsByCode(request.code())) {
            throw new DuplicateCouponCodeException("Coupon code already exists: " + request.code());
        }

        Coupon coupon = Coupon.builder()
                .seller(seller)
                .code(request.code())
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .validUntil(request.validUntil())
                .usageLimit(request.usageLimit())
                .build();

        Coupon saved = couponRepository.save(coupon);
        return toCouponResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> getCoupons(UUID userId) {
        Seller seller = findSellerByUserId(userId);
        validateApproved(seller);

        return couponRepository.findBySellerId(seller.getId())
                .stream()
                .map(this::toCouponResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCoupon(UUID userId, UUID couponId) {
        Seller seller = findSellerByUserId(userId);
        validateApproved(seller);

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException("Coupon not found with id: " + couponId));

        if (!coupon.getSeller().getId().equals(seller.getId())) {
            throw new UnauthorizedCouponAccessException("Coupon does not belong to this seller");
        }

        couponRepository.delete(coupon);
    }


    @Override
    @Transactional
    public CouponValidationResponse validateCoupon(String code, BigDecimal orderTotal) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new RuntimeException(
                        "Coupon not found or inactive: " + code));

        if (coupon.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Coupon has expired: " + code);
        }

        if (coupon.getTimesUsed() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon usage limit reached: " + code);
        }

        BigDecimal discountAmount;
        if (coupon.getDiscountType() == DiscountType.PERCENT) {
            discountAmount = orderTotal.multiply(
                    coupon.getDiscountValue()
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
            );
        } else {
            discountAmount = coupon.getDiscountValue()
                    .min(orderTotal);
        }

        coupon.setTimesUsed(coupon.getTimesUsed() + 1);
        couponRepository.save(coupon);

        return CouponValidationResponse.builder()
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .discountAmount(discountAmount)
                .valid(true)
                .build();
    }



    @Override
    public SellerInternalResponse getSellerInternal(UUID sellerId) {

        Seller seller = sellerRepository.findByUserId(sellerId)
                .orElseThrow(() ->
                        new SellerNotFoundException("Seller not found: " + sellerId));

        // CALL auth-service
        var user = authServiceClient.getUserById(seller.getUserId());

        return SellerInternalResponse.builder()
                .id(seller.getId())
                .userId(seller.getUserId())
                .businessName(seller.getBusinessName())
                .commissionRate(seller.getCommissionRate())
                .status(seller.getStatus().name())

                .email(user.email())
                .fullName(user.fullName())

                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SellerResponse> getAllSellers(Pageable pageable) {
        return sellerRepository.findAll(pageable)
                .map(this::toSellerResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerResponse> getPendingSellers() {
        return sellerRepository.findByStatus(SellerStatus.PENDING)
                .stream()
                .map(this::toSellerResponse)
                .toList();
    }

    @Override
    @Transactional
    public void approveSeller(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(
                        "Seller not found with id: " + sellerId));
        seller.setStatus(SellerStatus.APPROVED);
        sellerRepository.save(seller);
    }

    @Override
    @Transactional
    public void rejectSeller(UUID sellerId) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(
                        "Seller not found with id: " + sellerId));
        seller.setStatus(SellerStatus.REJECTED);
        sellerRepository.save(seller);
    }

    @Override
    @Transactional
    public void updateCommission(UUID sellerId, BigDecimal commissionRate) {
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException(
                        "Seller not found with id: " + sellerId));
        seller.setCommissionRate(commissionRate);
        sellerRepository.save(seller);
    }



    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Seller findSellerByUserId(UUID userId) {
        return sellerRepository.findByUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found for userId: " + userId));
    }

    private SellerResponse toSellerResponse(Seller seller) {
        return SellerResponse.builder()
                .id(seller.getId())
                .userId(seller.getUserId())
                .businessName(seller.getBusinessName())
                .gstNumber(seller.getGstNumber())
                .phone(seller.getPhone())
                .status(seller.getStatus())
                .commissionRate(seller.getCommissionRate())
                .storeName(seller.getStoreName())
                .storeDescription(seller.getStoreDescription())
                .logoUrl(seller.getLogoUrl())
                .createdAt(seller.getCreatedAt())
                .build();
    }

    private CouponResponse toCouponResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .sellerId(coupon.getSeller().getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .validUntil(coupon.getValidUntil())
                .usageLimit(coupon.getUsageLimit())
                .timesUsed(coupon.getTimesUsed())
                .isActive(coupon.getIsActive())
                .build();
    }
}