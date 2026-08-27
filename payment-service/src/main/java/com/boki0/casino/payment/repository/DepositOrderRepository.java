package com.boki0.casino.payment.repository;

import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.PaymentProviderType;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepositOrderRepository extends JpaRepository<DepositOrder, UUID> {

    Optional<DepositOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<DepositOrder> findByProviderAndProviderSessionId(
            PaymentProviderType provider,
            String providerSessionId
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("""
            select deposit
            from DepositOrder deposit
            where deposit.authUserId = :authUserId
              and (:fromCreatedAt is null or deposit.createdAt >= :fromCreatedAt)
              and (:toCreatedAt is null or deposit.createdAt <= :toCreatedAt)
            """)
    Page<DepositOrder> findPaymentHistory(
            @Param("authUserId") UUID authUserId,
            @Param("fromCreatedAt") LocalDateTime fromCreatedAt,
            @Param("toCreatedAt") LocalDateTime toCreatedAt,
            Pageable pageable
    );
}
