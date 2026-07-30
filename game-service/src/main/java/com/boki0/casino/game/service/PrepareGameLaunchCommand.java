package com.boki0.casino.game.service;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record PrepareGameLaunchCommand(
        UUID gameId,
        UUID playerId,
        String currency
) {

    public PrepareGameLaunchCommand {
        Objects.requireNonNull(gameId, "gameId must not be null");
        Objects.requireNonNull(playerId, "playerId must not be null");

        currency = Objects.requireNonNull(currency, "currency must not be null")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (currency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
    }
}
