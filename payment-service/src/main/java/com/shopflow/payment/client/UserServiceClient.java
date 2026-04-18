package com.shopflow.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/users/internal/profile/{userId}")
    UserProfileResponse getUserProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Id") String requestingUserId,
            @RequestHeader("X-User-Role") String requestingUserRole
    );

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    class UserProfileResponse {
        private UUID id;
        private String email;
        private String fullName;
    }
}