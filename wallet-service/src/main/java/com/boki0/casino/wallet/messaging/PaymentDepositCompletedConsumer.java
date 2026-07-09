package com.boki0.casino.wallet.messaging;

import com.boki0.casino.wallet.config.RabbitMQConfig;
import com.boki0.casino.wallet.dto.CreditWalletRequest;
import com.boki0.casino.wallet.dto.WalletTransactionResponse;
import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.event.PaymentDepositCompletedEvent;
import com.boki0.casino.wallet.service.WalletService;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentDepositCompletedConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentDepositCompletedConsumer.class);
    private static final String EVENT_TYPE_PAYMENT_DEPOSIT_COMPLETED = "PAYMENT_DEPOSIT_COMPLETED";

    private final WalletService walletService;

    public PaymentDepositCompletedConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PAYMENT_DEPOSIT_COMPLETED)
    public void consume(PaymentDepositCompletedEvent event) {
        LOGGER.info(
                "Received PaymentDepositCompletedEvent eventId={}, depositOrderId={}, authUserId={}",
                event.eventId(),
                event.depositOrderId(),
                event.authUserId()
        );

        try {
            validateEvent(event);

            WalletTransactionResponse response = walletService.credit(new CreditWalletRequest(
                    event.authUserId(),
                    event.creditsAmount(),
                    WalletReferenceType.PAYMENT,
                    event.depositOrderId().toString(),
                    "payment-deposit-" + event.depositOrderId(),
                    "Payment deposit completed: " + event.provider()
            ));

            LOGGER.info(
                    "Credited wallet for PaymentDepositCompletedEvent eventId={}, depositOrderId={}, transactionId={}",
                    event.eventId(),
                    event.depositOrderId(),
                    response.id()
            );
        } catch (Exception exception) {
            LOGGER.error(
                    "Failed to process PaymentDepositCompletedEvent eventId={}, depositOrderId={}",
                    event.eventId(),
                    event.depositOrderId(),
                    exception
            );
            throw exception;
        }
    }

    private void validateEvent(PaymentDepositCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("PaymentDepositCompletedEvent must not be null");
        }
        if (!EVENT_TYPE_PAYMENT_DEPOSIT_COMPLETED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported payment event type: " + event.eventType());
        }
        if (event.depositOrderId() == null) {
            throw new IllegalArgumentException("PaymentDepositCompletedEvent depositOrderId must not be null");
        }
        if (event.authUserId() == null) {
            throw new IllegalArgumentException("PaymentDepositCompletedEvent authUserId must not be null");
        }
        if (event.creditsAmount() == null || event.creditsAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("PaymentDepositCompletedEvent creditsAmount must be positive");
        }
    }
}
