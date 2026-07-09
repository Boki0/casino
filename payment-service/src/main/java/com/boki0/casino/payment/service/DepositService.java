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
import com.boki0.casino.payment.provider.PaymentEventType;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.PaymentProviderRegistry;
import com.boki0.casino.payment.provider.VerifiedPaymentEvent;
import com.boki0.casino.payment.repository.DepositOrderRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DepositService.class);

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

        DepositOrder savedDepositOrder = completeDepositAndPublishEvent(depositOrder);

        return toDepositResponse(savedDepositOrder);
    }

    @Transactional
    public DepositResponse handleVerifiedPaymentEvent(VerifiedPaymentEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("VerifiedPaymentEvent must not be null");
        }
        LOGGER.info(
                "Handling verified payment event provider={} eventType={} providerSessionId={} depositOrderId={}",
                event.provider(),
                event.eventType(),
                event.providerSessionId(),
                event.depositOrderId()
        );

        if (event.eventType() == PaymentEventType.UNKNOWN) {
            LOGGER.info("Ignoring UNKNOWN payment event provider={} metadata={}", event.provider(), event.metadata());
            return null;
        }

        if (event.eventType() != PaymentEventType.DEPOSIT_COMPLETED) {
            throw new IllegalArgumentException("Unsupported payment event type: " + event.eventType());
        }

        DepositOrder depositOrder = findDepositOrderForPaymentEvent(event);
        LOGGER.info(
                "Found deposit order id={} provider={} status={}",
                depositOrder.getId(),
                depositOrder.getProvider(),
                depositOrder.getStatus()
        );

        if (depositOrder.getProvider() != PaymentProviderType.STRIPE) {
            throw new IllegalArgumentException("Payment event does not belong to a STRIPE deposit order");
        }

        if (depositOrder.getStatus() == DepositStatus.COMPLETED) {
            LOGGER.info("Deposit order id={} already COMPLETED; webhook is idempotent", depositOrder.getId());
            return toDepositResponse(depositOrder);
        }

        if (depositOrder.getStatus() != DepositStatus.PENDING) {
            throw new IllegalArgumentException("Only PENDING deposit orders can be completed from payment events");
        }

        validatePaymentEventAmountAndCurrency(depositOrder, event);
        depositOrder.setProviderPaymentId(event.providerPaymentId());

        DepositOrder savedDepositOrder = completeDepositAndPublishEvent(depositOrder);

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

    private DepositOrder findDepositOrderForPaymentEvent(VerifiedPaymentEvent event) {
        Optional<DepositOrder> depositOrder = Optional.empty();

        if (event.providerSessionId() != null && !event.providerSessionId().isBlank()) {
            depositOrder = depositOrderRepository.findByProviderAndProviderSessionId(
                    event.provider(),
                    event.providerSessionId()
            );
            LOGGER.info(
                    "Deposit lookup by providerSessionId provider={} providerSessionId={} found={}",
                    event.provider(),
                    event.providerSessionId(),
                    depositOrder.isPresent()
            );
        }

        if (depositOrder.isEmpty() && event.depositOrderId() != null) {
            depositOrder = depositOrderRepository.findById(event.depositOrderId());
            LOGGER.info(
                    "Deposit lookup by id depositOrderId={} found={}",
                    event.depositOrderId(),
                    depositOrder.isPresent()
            );
        }

        return depositOrder.orElseThrow(() -> new PaymentResourceNotFoundException(
                "Deposit order not found for payment event"
        ));
    }

    private void validatePaymentEventAmountAndCurrency(DepositOrder depositOrder, VerifiedPaymentEvent event) {
        if (event.amount() != null && depositOrder.getAmount().compareTo(event.amount()) != 0) {
            LOGGER.warn(
                    "Stripe amount validation failed depositOrderId={} expectedAmount={} eventAmount={}",
                    depositOrder.getId(),
                    depositOrder.getAmount(),
                    event.amount()
            );
            throw new IllegalArgumentException("Stripe payment amount does not match deposit order amount");
        }

        if (
                event.currency() != null
                        && !depositOrder.getCurrency().equalsIgnoreCase(event.currency())
        ) {
            LOGGER.warn(
                    "Stripe currency validation failed depositOrderId={} expectedCurrency={} eventCurrency={}",
                    depositOrder.getId(),
                    depositOrder.getCurrency(),
                    event.currency()
            );
            throw new IllegalArgumentException("Stripe payment currency does not match deposit order currency");
        }

        LOGGER.info(
                "Stripe amount/currency validation passed depositOrderId={} amount={} currency={}",
                depositOrder.getId(),
                event.amount(),
                event.currency()
        );
    }

    private DepositOrder completeDepositAndPublishEvent(DepositOrder depositOrder) {
        LOGGER.info("Marking deposit order id={} as COMPLETED", depositOrder.getId());
        depositOrder.setStatus(DepositStatus.COMPLETED);
        depositOrder.setCompletedAt(LocalDateTime.now());

        DepositOrder savedDepositOrder = depositOrderRepository.save(depositOrder);
        domainEventPublisher.publish(toPaymentDepositCompletedEvent(savedDepositOrder));
        LOGGER.info("Published PaymentDepositCompletedEvent for depositOrderId={}", savedDepositOrder.getId());

        return savedDepositOrder;
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
