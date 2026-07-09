package com.boki0.casino.payment.service;

import com.boki0.casino.payment.dto.CreateDepositRequest;
import com.boki0.casino.payment.dto.DepositResponse;
import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.DepositStatus;
import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.event.DomainEventPublisher;
import com.boki0.casino.payment.event.PaymentDepositCompletedEvent;
import com.boki0.casino.payment.exception.PaymentAccessDeniedException;
import com.boki0.casino.payment.exception.PaymentResourceNotFoundException;
import com.boki0.casino.payment.provider.CreateCheckoutCommand;
import com.boki0.casino.payment.provider.CreateCheckoutResult;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.PaymentProviderRegistry;
import com.boki0.casino.payment.repository.DepositOrderRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private final DepositOrderRepository depositOrderRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;
    private final DomainEventPublisher domainEventPublisher;

    public DepositService(
            DepositOrderRepository depositOrderRepository,
            PaymentProviderRegistry paymentProviderRegistry,
            DomainEventPublisher domainEventPublisher
    ) {
        this.depositOrderRepository = depositOrderRepository;
        this.paymentProviderRegistry = paymentProviderRegistry;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public DepositResponse createDeposit(UUID authUserId, CreateDepositRequest request) {
        if (authUserId == null) {
            throw new IllegalArgumentException("authUserId must not be null");
        }

        return depositOrderRepository.findByIdempotencyKey(request.idempotencyKey())
                .map(this::toDepositResponse)
                .orElseGet(() -> createNewDeposit(authUserId, request));
    }

    @Transactional
    public DepositResponse completeManualDeposit(UUID authUserId, UUID depositId) {
        if (authUserId == null) {
            throw new IllegalArgumentException("authUserId must not be null");
        }
        if (depositId == null) {
            throw new IllegalArgumentException("depositId must not be null");
        }

        DepositOrder depositOrder = depositOrderRepository.findById(depositId)
                .orElseThrow(() -> new PaymentResourceNotFoundException(
                        "Deposit order not found: " + depositId
                ));

        if (!depositOrder.getAuthUserId().equals(authUserId)) {
            throw new PaymentAccessDeniedException("Deposit order does not belong to authenticated user");
        }

        if (depositOrder.getProvider() != PaymentProviderType.MANUAL) {
            throw new IllegalArgumentException("Only MANUAL deposit orders can be completed manually");
        }

        if (depositOrder.getStatus() == DepositStatus.COMPLETED) {
            return toDepositResponse(depositOrder);
        }

        if (depositOrder.getStatus() != DepositStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Only PENDING deposit orders can be completed manually"
            );
        }

        depositOrder.setStatus(DepositStatus.COMPLETED);
        depositOrder.setCompletedAt(LocalDateTime.now());

        DepositOrder savedDepositOrder = depositOrderRepository.save(depositOrder);
        domainEventPublisher.publish(toPaymentDepositCompletedEvent(savedDepositOrder));

        return toDepositResponse(savedDepositOrder);
    }

    private DepositResponse createNewDeposit(UUID authUserId, CreateDepositRequest request) {
        PaymentProvider paymentProvider = paymentProviderRegistry.getProvider(request.provider());
        String currency = request.currency().toUpperCase(Locale.ROOT);

        DepositOrder depositOrder = new DepositOrder();
        depositOrder.setAuthUserId(authUserId);
        depositOrder.setAmount(request.amount());
        depositOrder.setCurrency(currency);
        depositOrder.setCreditsAmount(request.creditsAmount());
        depositOrder.setStatus(DepositStatus.PENDING);
        depositOrder.setProvider(request.provider());
        depositOrder.setIdempotencyKey(request.idempotencyKey());

        DepositOrder savedDepositOrder = depositOrderRepository.save(depositOrder);

        CreateCheckoutCommand command = new CreateCheckoutCommand(
                savedDepositOrder.getId(),
                authUserId,
                savedDepositOrder.getAmount(),
                savedDepositOrder.getCurrency(),
                savedDepositOrder.getCreditsAmount(),
                savedDepositOrder.getIdempotencyKey(),
                request.successUrl(),
                request.cancelUrl()
        );

        CreateCheckoutResult checkoutResult = paymentProvider.createCheckoutSession(command);
        savedDepositOrder.setProviderSessionId(checkoutResult.providerSessionId());
        savedDepositOrder.setCheckoutUrl(checkoutResult.checkoutUrl());

        DepositOrder updatedDepositOrder = depositOrderRepository.save(savedDepositOrder);

        return toDepositResponse(updatedDepositOrder);
    }

    private PaymentDepositCompletedEvent toPaymentDepositCompletedEvent(DepositOrder depositOrder) {
        return new PaymentDepositCompletedEvent(
                UUID.randomUUID(),
                PaymentDepositCompletedEvent.EVENT_TYPE,
                PaymentDepositCompletedEvent.EVENT_VERSION,
                depositOrder.getId(),
                depositOrder.getAuthUserId(),
                depositOrder.getAmount(),
                depositOrder.getCurrency(),
                depositOrder.getCreditsAmount(),
                depositOrder.getProvider(),
                Instant.now()
        );
    }

    private DepositResponse toDepositResponse(DepositOrder depositOrder) {
        return new DepositResponse(
                depositOrder.getId(),
                depositOrder.getAuthUserId(),
                depositOrder.getAmount(),
                depositOrder.getCurrency(),
                depositOrder.getCreditsAmount(),
                depositOrder.getStatus(),
                depositOrder.getProvider(),
                depositOrder.getProviderSessionId(),
                depositOrder.getProviderPaymentId(),
                depositOrder.getCheckoutUrl(),
                depositOrder.getCreatedAt(),
                depositOrder.getUpdatedAt(),
                depositOrder.getCompletedAt()
        );
    }
}
