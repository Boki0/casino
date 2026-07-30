package com.boki0.casino.game.provider.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;

public record ProviderAuthenticateRequest(
        @NotBlank String token,
        @NotBlank String providerCode,
        @NotBlank String gameCode,
        @NotBlank String sessionId
) {

    public ProviderAuthenticateRequest {
        token = requireToken(token);
        providerCode = requireIdentifier(providerCode, "providerCode");
        gameCode = requireIdentifier(gameCode, "gameCode");
        sessionId = requireIdentifier(sessionId, "sessionId");
    }

    private static String requireToken(String token) {
        String requiredToken = Objects.requireNonNull(token, "token must not be null");
        if (requiredToken.isBlank()) {
            throw new IllegalArgumentException("token must not be blank");
        }
        return requiredToken;
    }

    private static String requireIdentifier(String value, String fieldName) {
        String normalizedValue = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }
}
