package com.boki0.casino.user.controller;

import com.boki0.casino.user.dto.CreateUserProfileRequest;
import com.boki0.casino.user.dto.UpdateUserProfileRequest;
import com.boki0.casino.user.dto.UserProfileResponse;
import com.boki0.casino.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/internal/users")
    public ResponseEntity<UserProfileResponse> createProfile(
            @Valid @RequestBody CreateUserProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.createProfile(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @RequestHeader("X-Auth-User-Id") UUID authUserId
    ) {
        UserProfileResponse response = userProfileService.getProfileByAuthUserId(authUserId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUserProfile(
            @RequestHeader("X-Auth-User-Id") UUID authUserId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserProfileResponse response = userProfileService.updateProfile(authUserId, request);
        return ResponseEntity.ok(response);
    }
}
