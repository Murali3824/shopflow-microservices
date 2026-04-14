package com.shopflow.seller.repository;

import com.shopflow.seller.entity.Seller;
import com.shopflow.seller.entity.status.SellerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerRepository extends JpaRepository<Seller, UUID> {

    Optional<Seller> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    boolean existsByGstNumber(String gstNumber);

    List<Seller> findByStatus(SellerStatus status);

}