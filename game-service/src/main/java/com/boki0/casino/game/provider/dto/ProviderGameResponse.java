package com.boki0.casino.game.provider.dto;

import java.math.BigDecimal;
import java.util.Set;

public record ProviderGameResponse(
        String gameCode,
        String providerCode,
        String name,
        String category,
        boolean active,
        Set<String> supportedCurrencies,
        Set<String> supportedPlatforms,
        BigDecimal minBet,
        BigDecimal maxBet,
        String imgUrl
) {
}
