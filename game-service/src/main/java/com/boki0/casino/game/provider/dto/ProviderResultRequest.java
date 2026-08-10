package com.boki0.casino.game.provider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderResultRequest(
        @NotNull UUID userId,
        @NotBlank String gameId,
        @NotBlank String roundId,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal amount,
        @NotBlank String reference,
        @NotBlank String providerId,
        @NotNull Long timestamp,
        String roundDetails,
        String platform,
        @NotBlank String token,
        String bonusCode,
        BigDecimal promoWinAmount,
        String promoWinReference,
        @JsonProperty("promoCampaignID") String promoCampaignId,
        String promoCampaignType
) {
}
