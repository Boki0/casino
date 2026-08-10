package com.boki0.casino.game.provider.dto;

import java.util.Locale;
import java.util.Objects;

public record ProviderLaunchRequest(
        String providerCode,
        String gameCode,
        String playerId,
        String currency,
        String token,
        String mode
) {

    public ProviderLaunchRequest {
        providerCode = requireNonBlank(providerCode, "providerCode");
        gameCode = requireNonBlank(gameCode, "gameCode");
        playerId = requireNonBlank(playerId, "playerId");
        currency = requireNonBlank(currency, "currency").toUpperCase(Locale.ROOT);
        token = requireNonBlank(token, "token");
        mode = requireNonBlank(mode, "mode").toUpperCase(Locale.ROOT);
    }

    private static String requireNonBlank(String value, String fieldName) {
        String normalizedValue = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }
}
