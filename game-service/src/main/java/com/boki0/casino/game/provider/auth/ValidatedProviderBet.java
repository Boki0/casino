package com.boki0.casino.game.provider.auth;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatedProviderBet(
        UUID localSessionId,
        UUID playerId,
        String currency,
        String providerCode,
        String gameCode,
        String providerSessionId,
        String roundId,
        String reference,
        BigDecimal amount
) {
}
