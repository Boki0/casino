package com.boki0.casino.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only wallet balance contract for trusted backend services.
 *
 * @param bonus always zero until the wallet domain supports a separate bonus balance
 */
public record InternalWalletBalanceResponse(
        UUID playerId,
        String currency,
        BigDecimal cash,
        BigDecimal bonus
) {
}
