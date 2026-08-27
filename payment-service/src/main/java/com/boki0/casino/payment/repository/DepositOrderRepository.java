package com.boki0.casino.payment.repository;

import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.PaymentProviderType;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface DepositOrderRepository extends JpaRepository<DepositOrder, UUID> {

    Optional<DepositOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<DepositOrder> findByProviderAndProviderSessionId(
            PaymentProviderType provider,
            String providerSessionId
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select deposit from DepositOrder deposit where deposit.id = :id")
    Optional<DepositOrder> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select deposit
            from DepositOrder deposit
            where deposit.authUserId = :authUserId
              and deposit.createdAt >= coalesce(:fromCreatedAt, deposit.createdAt)
              and deposit.createdAt <= coalesce(:toCreatedAt, deposit.createdAt)
            """)
    Page<DepositOrder> findPaymentHistory(
            @Param("authUserId") UUID authUserId,
            @Param("fromCreatedAt") LocalDateTime fromCreatedAt,
            @Param("toCreatedAt") LocalDateTime toCreatedAt,
            Pageable pageable
    );
}
