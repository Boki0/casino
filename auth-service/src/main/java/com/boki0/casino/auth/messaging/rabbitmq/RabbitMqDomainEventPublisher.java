package com.boki0.casino.auth.messaging.rabbitmq;

import com.boki0.casino.auth.event.DomainEvent;
import com.boki0.casino.auth.event.DomainEventPublisher;
import com.boki0.casino.auth.event.UserRegisteredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMqDomainEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitMqDomainEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event instanceof UserRegisteredEvent) {
            LOGGER.info(
                    "Publishing eventType={} eventId={} routingKey={}",
                    event.eventType(),
                    event.eventId(),
                    RabbitMQConfig.ROUTING_KEY_USER_REGISTERED
            );
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    RabbitMQConfig.ROUTING_KEY_USER_REGISTERED,
                    event
            );
            return;
        }

        throw new IllegalArgumentException("Unsupported event type: " + event.eventType());
    }
}
