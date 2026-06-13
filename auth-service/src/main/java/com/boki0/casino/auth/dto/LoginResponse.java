package com.boki0.casino.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
}
