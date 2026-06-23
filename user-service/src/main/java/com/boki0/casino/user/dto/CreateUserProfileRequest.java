package com.boki0.casino.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserProfileRequest(
        @NotNull
        UUID authUserId,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 3, max = 30)
        String username
) {
}
