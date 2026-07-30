package com.boki0.casino.game.service;

import java.time.Instant;
import java.util.UUID;

public record PreparedGameLaunch(
        UUID localSessionId,
        String rawToken,
        String providerCode,
        String gameCode,
        UUID playerId,
        String currency,
        Instant expiresAt
) {

    @Override
    public String toString() {
        return "PreparedGameLaunch[localSessionId=" + localSessionId
                + ", rawToken=<redacted>"
                + ", providerCode=" + providerCode
                + ", gameCode=" + gameCode
                + ", playerId=" + playerId
                + ", currency=" + currency
                + ", expiresAt=" + expiresAt
                + "]";
    }
}
