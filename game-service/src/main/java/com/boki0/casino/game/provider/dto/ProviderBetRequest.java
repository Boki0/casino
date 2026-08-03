package com.boki0.casino.game.provider.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderBetRequest(
        @NotNull UUID userId,
        @NotBlank String gameId,
        @NotBlank String roundId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String reference,
        @NotBlank String providerId,
        @NotNull Long timestamp,
        String roundDetails,
        String platform,
        String language,
        @NotBlank String token,
        String ipAddress
) {
}
