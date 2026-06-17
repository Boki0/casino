package com.boki0.casino.user.service;

import com.boki0.casino.user.dto.CreateUserProfileRequest;
import com.boki0.casino.user.dto.UpdateUserProfileRequest;
import com.boki0.casino.user.dto.UserProfileResponse;
import com.boki0.casino.user.entity.UserProfile;
import com.boki0.casino.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final String REF_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int REF_CODE_LENGTH = 8;

    private final UserProfileRepository userProfileRepository;
    private final SecureRandom secureRandom;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
        this.secureRandom = new SecureRandom();
    }

    public UserProfileResponse createProfile(CreateUserProfileRequest request) {
        if (userProfileRepository.existsByAuthUserId(request.authUserId())) {
            throw new IllegalArgumentException("User profile already exists");
        }

        if (userProfileRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email is already used");
        }

        if (userProfileRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username is already used");
        }

        String refCode = generateRefCode();
        UserProfile profile = new UserProfile(
                request.authUserId(),
                request.email(),
                request.username(),
                refCode
        );
        UserProfile savedProfile = userProfileRepository.save(profile);

        return toResponse(savedProfile);
    }

    public UserProfileResponse getProfileByAuthUserId(UUID authUserId) {
        UserProfile profile = getProfileOrThrow(authUserId);

        return toResponse(profile);
    }

    public UserProfileResponse updateProfile(UUID authUserId, UpdateUserProfileRequest request) {
        UserProfile profile = getProfileOrThrow(authUserId);

        profile.setDisplayName(request.displayName());
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setCountry(request.country());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setAvatarUrl(request.avatarUrl());

        UserProfile savedProfile = userProfileRepository.save(profile);

        return toResponse(savedProfile);
    }

    private UserProfile getProfileOrThrow(UUID authUserId) {
        return userProfileRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("User profile not found"));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getAuthUserId(),
                profile.getEmail(),
                profile.getUsername(),
                profile.getDisplayName(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getCountry(),
                profile.getPhoneNumber(),
                profile.getDateOfBirth(),
                profile.getAvatarUrl(),
                profile.getRefCode(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String generateRefCode() {
        String refCode;
        do {
            refCode = randomRefCode();
        } while (userProfileRepository.existsByRefCode(refCode));

        return refCode;
    }

    private String randomRefCode() {
        StringBuilder refCode = new StringBuilder(REF_CODE_LENGTH);
        for (int index = 0; index < REF_CODE_LENGTH; index++) {
            int characterIndex = secureRandom.nextInt(REF_CODE_CHARACTERS.length());
            refCode.append(REF_CODE_CHARACTERS.charAt(characterIndex));
        }

        return refCode.toString();
    }
}
