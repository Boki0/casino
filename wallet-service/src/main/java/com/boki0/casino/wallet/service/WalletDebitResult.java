package com.boki0.casino.wallet.service;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDebitResult(
        UUID transactionId,
        UUID playerId,
        String currency,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String externalReference,
        boolean duplicate
) {
}
