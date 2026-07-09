package com.boki0.casino.payment.messaging;

import com.boki0.casino.payment.config.RabbitMQConfig;
import com.boki0.casino.payment.event.DomainEvent;
import com.boki0.casino.payment.event.DomainEventPublisher;
import com.boki0.casino.payment.event.PaymentDepositCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitDomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitDomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof PaymentDepositCompletedEvent) {
            LOGGER.info(
                    "Publishing eventType={} eventId={} routingKey={}",
                    event.eventType(),
                    event.eventId(),
                    RabbitMQConfig.ROUTING_KEY_PAYMENT_DEPOSIT_COMPLETED
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_PAYMENT_DEPOSIT_COMPLETED,
                    event
            );
            return;
        }

        throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
    }
}
