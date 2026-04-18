package com.shopflow.payment.repository;

import com.shopflow.payment.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByReturnRequestId(UUID returnRequestId);

    boolean existsByReturnRequestId(UUID returnRequestId);
}