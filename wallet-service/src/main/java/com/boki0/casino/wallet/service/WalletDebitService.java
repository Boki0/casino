package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.WalletTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WalletDebitService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletDebitService.class);

    private final WalletDebitTransactionalExecutor transactionalExecutor;
    private final WalletDebitTransactionLookupService lookupService;
    private final WalletDebitTransactionResolver transactionResolver;

    public WalletDebitService(
            WalletDebitTransactionalExecutor transactionalExecutor,
            WalletDebitTransactionLookupService lookupService,
            WalletDebitTransactionResolver transactionResolver
    ) {
        this.transactionalExecutor = transactionalExecutor;
        this.lookupService = lookupService;
        this.transactionResolver = transactionResolver;
    }

    public WalletDebitResult debit(WalletDebitCommand command) {
        try {
            WalletDebitResult result = transactionalExecutor.debitOnce(command);
            LOGGER.info(
                    "Wallet debit resolved transactionId={} playerId={} currency={} duplicate={}",
                    result.transactionId(),
                    result.playerId(),
                    result.currency(),
                    result.duplicate()
            );
            return result;
        } catch (DataIntegrityViolationException exception) {
            WalletTransaction existing = lookupService
                    .findByExternalReference(command.externalReference())
                    .orElseThrow(() -> exception);
            LOGGER.info(
                    "Resolved concurrent wallet debit for externalReference={}",
                    command.externalReference()
            );
            return transactionResolver.resolveDuplicate(existing, command);
        }
    }
}
