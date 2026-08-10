package com.boki0.casino.wallet.dto;

import com.boki0.casino.wallet.service.WalletDebitResult;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalWalletDebitResponse(
        UUID transactionId,
        UUID playerId,
        String currency,
        BigDecimal amount,
        BigDecimal cash,
        BigDecimal bonus,
        String reference,
        boolean duplicate
) {

    public static InternalWalletDebitResponse from(WalletDebitResult result) {
        return new InternalWalletDebitResponse(
                result.transactionId(),
                result.playerId(),
                result.currency(),
                result.amount(),
                result.balanceAfter(),
                BigDecimal.ZERO,
                result.externalReference(),
                result.duplicate()
        );
    }
}
