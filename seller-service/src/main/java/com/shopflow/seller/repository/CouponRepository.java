package com.shopflow.seller.repository;

import com.shopflow.seller.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {

    List<Coupon> findBySellerId(UUID sellerId);

    Optional<Coupon> findByCode(String code);

    boolean existsByCode(String code);

    Optional<Coupon>findByCodeAndIsActiveTrue(String code);
}