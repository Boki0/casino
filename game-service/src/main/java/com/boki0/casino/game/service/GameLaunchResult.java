package com.boki0.casino.game.service;

import java.util.Objects;
import java.util.UUID;

public record GameLaunchResult(
        UUID localSessionId,
        String launchUrl
) {

    public GameLaunchResult {
        Objects.requireNonNull(localSessionId, "localSessionId must not be null");
        launchUrl = requireNonBlank(launchUrl);
    }

    private static String requireNonBlank(String launchUrl) {
        String normalizedLaunchUrl = Objects.requireNonNull(
                launchUrl,
                "launchUrl must not be null"
        ).trim();
        if (normalizedLaunchUrl.isBlank()) {
            throw new IllegalArgumentException("launchUrl must not be blank");
        }
        return normalizedLaunchUrl;
    }
}
