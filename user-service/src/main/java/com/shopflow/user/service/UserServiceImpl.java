package com.shopflow.user.service;

import com.shopflow.user.dto.*;
import com.shopflow.user.entity.UserAddress;
import com.shopflow.user.entity.UserProfile;
import com.shopflow.user.exception.ResourceNotFoundException;
import com.shopflow.user.repository.UserAddressRepository;
import com.shopflow.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserProfileRepository profileRepository;
    private final UserAddressRepository addressRepository;
    private final MinioService minioService;

    @Override
    @Transactional
    public void createInitialProfile(UUID userId, String fullName,String email) {
        if (profileRepository.existsByUserId(userId)) {
            log.info("Profile already exists for userId: {} — skipping", userId);
            return;
        }

        UserProfile profile = UserProfile.builder()
                .userId(userId)
                .fullName(fullName)
                .email(email)
                .build();

        profileRepository.save(profile);
        log.info("Created initial profile for userId: {}", userId);
    }

    // ── Profile ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        UserProfile profile = findProfileByUserIdOrThrow(userId);
        return toProfileResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UserProfileRequest request) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .build());

        // manually apply only non-null fields from request
        if (request.getFullName() != null)    profile.setFullName(request.getFullName());
        if (request.getPhone() != null)       profile.setPhone(request.getPhone());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());

        return toProfileResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public UserProfileResponse uploadAvatar(UUID userId, MultipartFile file) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.builder()
                        .userId(userId)
                        .build());

        // delete old avatar before uploading new one
        if (profile.getAvatarUrl() != null) {
            minioService.deleteFile(profile.getAvatarUrl());
            log.info("Deleted old avatar for userId: {}", userId);
        }

        profile.setAvatarUrl(minioService.uploadFile(file));
        return toProfileResponse(profileRepository.save(profile));
    }


    // ── Addresses ────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getAddresses(UUID userId) {
        return addressRepository.findAllByUserId(userId)
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public UserAddressResponse addAddress(UUID userId, UserAddressRequest request) {
        if (request.isDefault()) {
            clearExistingDefault(userId);
        }

        UserAddress address = UserAddress.builder()
                .userId(userId)
                .label(request.getLabel())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(request.isDefault())
                .build();

        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public UserAddressResponse updateAddress(UUID userId, UUID addressId,
                                             UserAddressRequest request) {
        UserAddress address = findAddressByIdAndUserIdOrThrow(userId, addressId);

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(userId);
        }

        // manually apply only non-null fields from request
        if (request.getLabel() != null)   address.setLabel(request.getLabel());
        if (request.getStreet() != null)  address.setStreet(request.getStreet());
        if (request.getCity() != null)    address.setCity(request.getCity());
        if (request.getState() != null)   address.setState(request.getState());
        if (request.getPincode() != null) address.setPincode(request.getPincode());
        address.setDefault(request.isDefault());

        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        if (!addressRepository.existsByIdAndUserId(addressId, userId)) {
            throw new ResourceNotFoundException("Address not found for this user");
        }
        addressRepository.deleteByIdAndUserId(addressId, userId);
    }

    @Override
    @Transactional
    public UserAddressResponse setDefaultAddress(UUID userId, UUID addressId) {
        clearExistingDefault(userId);
        UserAddress address = findAddressByIdAndUserIdOrThrow(userId, addressId);
        address.setDefault(true);
        return toAddressResponse(addressRepository.save(address));
    }


    @Override
    public UserAddressResponse getDefaultAddress(UUID userId) {
        UserAddress address = addressRepository
                .findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No default address found for user: " + userId));

        return UserAddressResponse.builder()
                .id(address.getId())
                .label(address.getLabel())
                .street(address.getStreet())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.isDefault())
                .build();
    }


    // ── Manual converters ────────────────────────────────────

    private UserProfileResponse toProfileResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .email(p.getEmail())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .avatarUrl(p.getAvatarUrl())
                .dateOfBirth(p.getDateOfBirth())
                .build();
    }

    private UserAddressResponse toAddressResponse(UserAddress a) {
        return UserAddressResponse.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .label(a.getLabel())
                .street(a.getStreet())
                .city(a.getCity())
                .state(a.getState())
                .pincode(a.getPincode())
                .isDefault(a.isDefault())
                .build();
    }


    // ── Private helpers ──────────────────────────────────────

    private UserProfile findProfileByUserIdOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for userId: " + userId));
    }

    private UserAddress findAddressByIdAndUserIdOrThrow(UUID userId, UUID addressId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found for this user"));
    }

    private void clearExistingDefault(UUID userId) {
        addressRepository.findAllByUserId(userId)
                .stream()
                .filter(UserAddress::isDefault)
                .findFirst()
                .ifPresent(existing -> {
                    existing.setDefault(false);
                    addressRepository.save(existing);
                });
    }
}