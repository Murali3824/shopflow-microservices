package com.shopflow.seller.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "auth-service", url = "http://localhost:8081")
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/internal/{userId}")
    AuthUserResponse getUserById(@PathVariable UUID userId);
}