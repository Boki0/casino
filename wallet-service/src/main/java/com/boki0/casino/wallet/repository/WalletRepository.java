package com.boki0.casino.wallet.repository;

import com.boki0.casino.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByAuthUserId(UUID authUserId);

    boolean existsByAuthUserId(UUID authUserId);
}
