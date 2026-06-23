package com.boki0.casino.user.service;

import com.boki0.casino.user.dto.CreateUserProfileRequest;
import com.boki0.casino.user.dto.UpdateUserProfileRequest;
import com.boki0.casino.user.dto.UserProfileResponse;
import com.boki0.casino.user.entity.UserProfile;
import com.boki0.casino.user.event.UserRegisteredEvent;
import com.boki0.casino.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class UserProfileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserProfileService.class);
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

        UserProfile savedProfile = createAndSaveProfile(
                request.authUserId(),
                request.email(),
                request.username()
        );

        return toResponse(savedProfile);
    }

    public UserProfileResponse createProfileFromUserRegisteredEvent(UserRegisteredEvent event) {
        validateUserRegisteredEvent(event);

        return userProfileRepository.findByAuthUserId(event.authUserId())
                .map(existingProfile -> {
                    LOGGER.info(
                            "User profile already exists for UserRegisteredEvent eventId={}, authUserId={}",
                            event.eventId(),
                            event.authUserId()
                    );
                    return toResponse(existingProfile);
                })
                .orElseGet(() -> createProfileFromNewUserRegisteredEvent(event));
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

    private UserProfileResponse createProfileFromNewUserRegisteredEvent(UserRegisteredEvent event) {
        if (userProfileRepository.existsByEmail(event.email())) {
            throw new IllegalArgumentException("Email is already used by another user profile");
        }

        if (userProfileRepository.existsByUsername(event.username())) {
            throw new IllegalArgumentException("Username is already used by another user profile");
        }

        UserProfile savedProfile = createAndSaveProfile(
                event.authUserId(),
                event.email(),
                event.username()
        );
        LOGGER.info(
                "User profile created from UserRegisteredEvent eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );

        return toResponse(savedProfile);
    }

    private void validateUserRegisteredEvent(UserRegisteredEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("UserRegisteredEvent must not be null");
        }

        if (event.authUserId() == null) {
            throw new IllegalArgumentException("UserRegisteredEvent authUserId must not be null");
        }

        if (event.email() == null || event.email().isBlank()) {
            throw new IllegalArgumentException("UserRegisteredEvent email must not be blank");
        }

        if (event.username() == null || event.username().isBlank()) {
            throw new IllegalArgumentException("UserRegisteredEvent username must not be blank");
        }
    }

    private UserProfile createAndSaveProfile(UUID authUserId, String email, String username) {
        String refCode = generateRefCode();
        UserProfile profile = new UserProfile(
                authUserId,
                email,
                username,
                refCode
        );

        return userProfileRepository.save(profile);
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
