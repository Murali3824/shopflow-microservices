package com.shopflow.order.repository;

import com.shopflow.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {

    List<ReturnRequest> findByOrderId(UUID orderId);

    Optional<ReturnRequest> findByOrderItemId(UUID orderItemId);

    List<ReturnRequest> findByStatus(String status);

    List<ReturnRequest> findByOrderIdAndStatus(UUID orderId, String status);

    boolean existsByOrderItemId(UUID orderItemId);
}