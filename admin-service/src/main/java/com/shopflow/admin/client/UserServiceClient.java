package com.shopflow.admin.client;

import com.shopflow.admin.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

@FeignClient(name = "auth-service")  // FIXED: was "user-service"
public interface UserServiceClient {

    @GetMapping("/api/auth/users/internal/all")  // FIXED: correct path
    Page<UserResponse> getAllUsers(@SpringQueryMap Pageable pageable);

    @PutMapping("/api/auth/users/internal/{userId}/ban")  // FIXED: correct path
    void banUser(@PathVariable("userId") UUID userId);

    @PutMapping("/api/auth/users/internal/{userId}/unban")  // FIXED: correct path
    void unbanUser(@PathVariable("userId") UUID userId);
}