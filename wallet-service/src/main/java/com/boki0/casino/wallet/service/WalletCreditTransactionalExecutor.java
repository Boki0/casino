package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.Wallet;
import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletStatus;
import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WalletCreditTransactionalExecutor {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletCreditTransactionResolver transactionResolver;

    public WalletCreditTransactionalExecutor(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletCreditTransactionResolver transactionResolver
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.transactionResolver = transactionResolver;
    }

    @Transactional
    public WalletCreditResult creditOnce(WalletCreditCommand command) {
        Optional<WalletTransaction> existing =
                walletTransactionRepository.findByExternalReference(command.externalReference());
        if (existing.isPresent()) {
            return transactionResolver.resolveDuplicate(existing.get(), command);
        }

        Wallet wallet = walletRepository.findByAuthUserIdForUpdate(command.playerId())
                .orElseThrow(() -> WalletNotFoundException.forPlayerAndCurrency(
                        command.playerId(), command.currency()
                ));

        existing = walletTransactionRepository.findByExternalReference(command.externalReference());
        if (existing.isPresent()) {
            return transactionResolver.resolveDuplicate(existing.get(), command);
        }

        validateWallet(wallet, command);
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(command.amount());

        WalletTransaction transaction = new WalletTransaction(
                wallet.getId(),
                wallet.getAuthUserId(),
                WalletTransactionType.CREDIT,
                WalletTransactionStatus.COMPLETED,
                command.amount(),
                balanceBefore,
                wallet.getBalance(),
                wallet.getCurrency(),
                WalletReferenceType.GAME_WIN,
                command.externalReference(),
                command.externalReference()
        );

        walletRepository.save(wallet);
        WalletTransaction savedTransaction = walletTransactionRepository.saveAndFlush(transaction);
        return transactionResolver.resolveCreated(savedTransaction);
    }

    private void validateWallet(Wallet wallet, WalletCreditCommand command) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Wallet is not active");
        }
        if (!wallet.getCurrency().equals(command.currency())) {
            throw WalletNotFoundException.forPlayerAndCurrency(command.playerId(), command.currency());
        }
    }
}
