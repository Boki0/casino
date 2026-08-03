package com.boki0.casino.game.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletDebitRequest(
        UUID playerId,
        String currency,
        BigDecimal amount,
        String reference
) {
}
