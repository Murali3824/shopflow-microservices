package com.shopflow.admin.client;

import com.shopflow.admin.dto.SellerResponse;
import com.shopflow.admin.dto.UpdateCommissionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "seller-service")
public interface SellerServiceClient {

    @GetMapping("/api/sellers/internal/all")
    Page<SellerResponse> getAllSellers(@SpringQueryMap Pageable pageable);

    @GetMapping("/api/sellers/internal/pending")
    List<SellerResponse> getPendingSellers();

    @PutMapping("/api/sellers/internal/{sellerId}/approve")
    void approveSeller(@PathVariable("sellerId") UUID sellerId);

    @PutMapping("/api/sellers/internal/{sellerId}/reject")
    void rejectSeller(@PathVariable("sellerId") UUID sellerId);

    @PutMapping("/api/sellers/internal/{sellerId}/commission")
    void updateCommission(@PathVariable("sellerId") UUID sellerId,
                          @RequestBody UpdateCommissionRequest request);
}