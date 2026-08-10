package com.boki0.casino.wallet.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record WalletCreditCommand(
        UUID playerId,
        String currency,
        BigDecimal amount,
        String externalReference
) {

    public WalletCreditCommand {
        playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        currency = requireNonBlank(currency, "currency").toUpperCase(Locale.ROOT);
        amount = Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        externalReference = requireNonBlank(externalReference, "externalReference");
    }

    private static String requireNonBlank(String value, String fieldName) {
        String requiredValue = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (requiredValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return requiredValue;
    }
}
