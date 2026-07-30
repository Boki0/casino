package com.boki0.casino.game.api.dto;

import java.util.UUID;

public record GameLaunchResponse(
        UUID sessionId,
        String launchUrl
) {
}
