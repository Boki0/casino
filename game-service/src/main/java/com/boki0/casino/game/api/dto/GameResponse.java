package com.boki0.casino.game.api.dto;

import com.boki0.casino.game.domain.GameCategory;

import java.util.UUID;

public record GameResponse(
        UUID id,
        String name,
        String slug,
        String providerCode,
        GameCategory category,
        String thumbnailUrl
) {
}
