package com.boki0.casino.wallet.repository;

import com.boki0.casino.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByAuthUserId(UUID authUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.authUserId = :authUserId")
    Optional<Wallet> findByAuthUserIdForUpdate(@Param("authUserId") UUID authUserId);

    boolean existsByAuthUserId(UUID authUserId);
}
