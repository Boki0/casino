package com.boki0.casino.game.api.dto;

import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record GameResponse(
        UUID id,
        String name,
        String slug,
        String providerCode,
        GameCategory category,
        String thumbnailUrl,
        Set<String> supportedCurrencies,
        Set<GamePlatform> supportedPlatforms,
        BigDecimal minBet,
        BigDecimal maxBet
) {
}
