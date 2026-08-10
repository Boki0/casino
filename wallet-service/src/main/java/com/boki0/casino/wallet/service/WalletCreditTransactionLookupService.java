package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class WalletCreditTransactionLookupService {

    private final WalletTransactionRepository walletTransactionRepository;

    public WalletCreditTransactionLookupService(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public Optional<WalletTransaction> findByExternalReference(String externalReference) {
        return walletTransactionRepository.findByExternalReference(externalReference);
    }
}
