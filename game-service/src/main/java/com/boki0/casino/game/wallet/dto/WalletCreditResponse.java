package com.boki0.casino.game.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletCreditResponse(
        UUID transactionId,
        UUID playerId,
        String currency,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal cash,
        BigDecimal bonus,
        String reference,
        boolean duplicate
) {
}
