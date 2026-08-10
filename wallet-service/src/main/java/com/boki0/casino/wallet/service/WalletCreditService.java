package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.WalletTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class WalletCreditService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletCreditService.class);

    private final WalletCreditTransactionalExecutor transactionalExecutor;
    private final WalletCreditTransactionLookupService lookupService;
    private final WalletCreditTransactionResolver transactionResolver;

    public WalletCreditService(
            WalletCreditTransactionalExecutor transactionalExecutor,
            WalletCreditTransactionLookupService lookupService,
            WalletCreditTransactionResolver transactionResolver
    ) {
        this.transactionalExecutor = transactionalExecutor;
        this.lookupService = lookupService;
        this.transactionResolver = transactionResolver;
    }

    public WalletCreditResult credit(WalletCreditCommand command) {
        try {
            WalletCreditResult result = transactionalExecutor.creditOnce(command);
            LOGGER.info(
                    "Wallet credit resolved transactionId={} playerId={} currency={} duplicate={}",
                    result.transactionId(), result.playerId(), result.currency(), result.duplicate()
            );
            return result;
        } catch (DataIntegrityViolationException exception) {
            WalletTransaction existing = lookupService
                    .findByExternalReference(command.externalReference())
                    .orElseThrow(() -> exception);
            LOGGER.info("Resolved concurrent wallet credit for externalReference={}", command.externalReference());
            return transactionResolver.resolveDuplicate(existing, command);
        }
    }
}
