package com.boki0.casino.payment.service;

import com.boki0.casino.payment.dto.CreateDepositRequest;
import com.boki0.casino.payment.dto.DepositResponse;
import com.boki0.casino.payment.entity.DepositOrder;
import com.boki0.casino.payment.entity.DepositStatus;
import com.boki0.casino.payment.provider.CreateCheckoutCommand;
import com.boki0.casino.payment.provider.CreateCheckoutResult;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.PaymentProviderRegistry;
import com.boki0.casino.payment.repository.DepositOrderRepository;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private final DepositOrderRepository depositOrderRepository;
    private final PaymentProviderRegistry paymentProviderRegistry;

    public DepositService(
            DepositOrderRepository depositOrderRepository,
            PaymentProviderRegistry paymentProviderRegistry
    ) {
        this.depositOrderRepository = depositOrderRepository;
        this.paymentProviderRegistry = paymentProviderRegistry;
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
