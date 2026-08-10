package com.boki0.casino.game.provider.auth;

import java.util.UUID;

public record ValidatedProviderSession(
        UUID localSessionId,
        UUID playerId,
        String currency
) {
}
