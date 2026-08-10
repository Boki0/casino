package com.boki0.casino.wallet.dto;

import com.boki0.casino.wallet.service.WalletCreditResult;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalWalletCreditResponse(
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

    public static InternalWalletCreditResponse from(WalletCreditResult result) {
        return new InternalWalletCreditResponse(
                result.transactionId(),
                result.playerId(),
                result.currency(),
                result.amount(),
                result.balanceBefore(),
                result.balanceAfter(),
                BigDecimal.ZERO,
                result.externalReference(),
                result.duplicate()
        );
    }
}
