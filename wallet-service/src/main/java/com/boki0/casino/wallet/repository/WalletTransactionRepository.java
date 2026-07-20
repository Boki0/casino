package com.boki0.casino.wallet.repository;

import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<WalletTransaction> findByReferenceTypeAndReferenceId(
            WalletReferenceType referenceType,
            String referenceId
    );

    boolean existsByReferenceTypeAndReferenceId(WalletReferenceType referenceType, String referenceId);

    List<WalletTransaction> findByAuthUserIdOrderByCreatedAtDesc(UUID authUserId);

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
