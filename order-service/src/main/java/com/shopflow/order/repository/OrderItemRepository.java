package com.shopflow.order.repository;

import com.shopflow.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrderId(UUID orderId);

    List<OrderItem> findBySellerId(UUID sellerId);

    List<OrderItem> findByOrderIdAndSellerId(UUID orderId, UUID sellerId);

    List<OrderItem> findByProductId(UUID productId);

    boolean existsByOrderIdAndProductId(UUID orderId, UUID productId);
}