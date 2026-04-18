package com.shopflow.payment.repository;

import com.shopflow.payment.entity.SellerEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SellerEarningRepository extends JpaRepository<SellerEarning, UUID> {

    List<SellerEarning> findAllBySellerId(UUID sellerId);

    boolean existsByOrderItemId(UUID orderItemId);

    @Query("SELECT COALESCE(SUM(se.netEarning), 0) FROM SellerEarning se WHERE se.sellerId = :sellerId")
    BigDecimal sumNetEarningBySellerId(@Param("sellerId") UUID sellerId);

    void deleteByOrderId(UUID orderId);

    // ── Revenue report queries ─────────────────────────────────────

    @Query("SELECT COALESCE(SUM(e.grossAmount), 0) FROM SellerEarning e " +
            "WHERE e.createdAt BETWEEN :from AND :to")
    BigDecimal sumGrossAmountBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(e.commissionAmount), 0) FROM SellerEarning e " +
            "WHERE e.createdAt BETWEEN :from AND :to")
    BigDecimal sumCommissionAmountBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT e.orderId) FROM SellerEarning e " +
            "WHERE e.createdAt BETWEEN :from AND :to")
    Long countDistinctOrdersBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}