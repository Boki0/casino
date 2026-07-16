package com.boki0.casino.game.api.dto;

import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GameProvider;

import java.util.UUID;

public record GameResponse(
        UUID id,
        String name,
        String slug,
        GameProvider provider,
        GameCategory category,
        String thumbnailUrl
) {
}
