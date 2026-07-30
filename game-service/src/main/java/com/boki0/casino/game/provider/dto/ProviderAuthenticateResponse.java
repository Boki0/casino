package com.boki0.casino.game.provider.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProviderAuthenticateResponse(
        UUID userId,
        String currency,
        BigDecimal cash,
        BigDecimal bonus,
        int error,
        String description
) {

    public static ProviderAuthenticateResponse success(
            UUID userId,
            String currency,
            BigDecimal cash
    ) {
        return new ProviderAuthenticateResponse(
                userId,
                currency,
                cash,
                BigDecimal.ZERO,
                0,
                "Success"
        );
    }

    public static ProviderAuthenticateResponse error(int error, String description) {
        return new ProviderAuthenticateResponse(
                null,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                error,
                description
        );
    }
}
