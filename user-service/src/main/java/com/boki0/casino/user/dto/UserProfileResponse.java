package com.boki0.casino.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID authUserId,
        String email,
        String username,
        String displayName,
        String firstName,
        String lastName,
        String country,
        String phoneNumber,
        LocalDate dateOfBirth,
        String avatarUrl,
        String refCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
