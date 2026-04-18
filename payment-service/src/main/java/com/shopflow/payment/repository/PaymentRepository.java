package com.shopflow.payment.repository;

import com.shopflow.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByUserId(UUID userId);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
}