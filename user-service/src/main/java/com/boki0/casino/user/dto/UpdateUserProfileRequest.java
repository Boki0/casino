package com.boki0.casino.user.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserProfileRequest(
        @Size(max = 50)
        String displayName,

        @Size(max = 50)
        String firstName,

        @Size(max = 50)
        String lastName,

        @Size(max = 2)
        String country,

        @Size(max = 30)
        String phoneNumber,

        LocalDate dateOfBirth,

        @Size(max = 500)
        String avatarUrl
) {
}
