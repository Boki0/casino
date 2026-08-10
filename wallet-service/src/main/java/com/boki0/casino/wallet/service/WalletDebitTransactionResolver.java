package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;
import com.boki0.casino.wallet.exception.WalletTransactionIdempotencyConflictException;
import org.springframework.stereotype.Component;

@Component
public class WalletDebitTransactionResolver {

    public WalletDebitResult resolveDuplicate(
            WalletTransaction transaction,
            WalletDebitCommand command
    ) {
        if (transaction.getType() != WalletTransactionType.DEBIT
                || transaction.getStatus() != WalletTransactionStatus.COMPLETED
                || !transaction.getAuthUserId().equals(command.playerId())
                || !transaction.getCurrency().equals(command.currency())
                || transaction.getAmount().compareTo(command.amount()) != 0
                || !transaction.getExternalReference().equals(command.externalReference())) {
            throw new WalletTransactionIdempotencyConflictException(command.externalReference());
        }
        return toResult(transaction, true);
    }

    public WalletDebitResult resolveCreated(WalletTransaction transaction) {
        return toResult(transaction, false);
    }

    private WalletDebitResult toResult(WalletTransaction transaction, boolean duplicate) {
        return new WalletDebitResult(
                transaction.getId(),
                transaction.getAuthUserId(),
                transaction.getCurrency(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getExternalReference(),
                duplicate
        );
    }
}
