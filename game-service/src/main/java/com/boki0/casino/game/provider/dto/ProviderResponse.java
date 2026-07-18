package com.boki0.casino.game.provider.dto;

public record ProviderResponse(
        String providerCode,
        String name,
        boolean active
) {
}
