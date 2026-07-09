package com.boki0.casino.payment.repository;

import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.PaymentProviderType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositOrderRepository extends JpaRepository<DepositOrder, UUID> {

    Optional<DepositOrder> findByIdempotencyKey(String idempotencyKey);

    Optional<DepositOrder> findByProviderAndProviderSessionId(
            PaymentProviderType provider,
            String providerSessionId
    );

    boolean existsByIdempotencyKey(String idempotencyKey);
}
