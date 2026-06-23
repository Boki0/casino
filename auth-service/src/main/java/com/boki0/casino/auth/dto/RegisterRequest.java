package com.boki0.casino.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 8)
        String password,

        @NotBlank
        @Size(min = 3, max = 30)
        String username,

        String promoCode
) {
}
