package com.boki0.casino.game.provider.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderResultResponse(
        UUID transactionId,
        String currency,
        BigDecimal cash,
        BigDecimal bonus,
        int error,
        String description
) {

    public static ProviderResultResponse error(int error, String description) {
        return new ProviderResultResponse(
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                error,
                description
        );
    }
}
