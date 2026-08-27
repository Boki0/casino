package com.boki0.casino.payment.messaging;

import com.boki0.casino.payment.config.RabbitMQConfig;
import com.boki0.casino.payment.event.WalletDepositCreditedEvent;
import com.boki0.casino.payment.service.DepositService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class WalletDepositCreditedConsumer {

    private final DepositService depositService;

    public WalletDepositCreditedConsumer(DepositService depositService) {
        this.depositService = depositService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_WALLET_DEPOSIT_CREDITED)
    public void consume(WalletDepositCreditedEvent event) {
        depositService.recordWalletCredit(event);
    }
}
