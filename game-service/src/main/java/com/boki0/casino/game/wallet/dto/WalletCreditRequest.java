package com.boki0.casino.game.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletCreditRequest(
        UUID playerId,
        String currency,
        BigDecimal amount,
        String reference
) {
}
