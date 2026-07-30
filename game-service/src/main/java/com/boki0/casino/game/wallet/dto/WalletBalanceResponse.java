package com.boki0.casino.game.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceResponse(
        UUID playerId,
        String currency,
        BigDecimal cash,
        BigDecimal bonus
) {
}
