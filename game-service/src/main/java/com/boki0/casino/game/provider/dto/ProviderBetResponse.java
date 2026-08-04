package com.boki0.casino.game.provider.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderBetResponse(
        UUID transactionId,
        String currency,
        BigDecimal cash,
        BigDecimal bonus,
        BigDecimal usedPromo,
        int error,
        String description
) {

    public static ProviderBetResponse error(int error, String description) {
        return new ProviderBetResponse(
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                error,
                description
        );
    }
}
