// ── UserService.java ─────────────────────────────────────────
package com.shopflow.user.service;

import com.shopflow.user.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {

    void createInitialProfile(UUID userId, String fullName,String email);

    UserProfileResponse getProfile(UUID userId);

    UserProfileResponse updateProfile(UUID userId, UserProfileRequest request);

    UserProfileResponse uploadAvatar(UUID userId, MultipartFile file);

    List<UserAddressResponse> getAddresses(UUID userId);

    UserAddressResponse addAddress(UUID userId, UserAddressRequest request);

    UserAddressResponse updateAddress(UUID userId, UUID addressId, UserAddressRequest request);

    void deleteAddress(UUID userId, UUID addressId);

    UserAddressResponse setDefaultAddress(UUID userId, UUID addressId);

    UserAddressResponse getDefaultAddress(UUID userId);
}