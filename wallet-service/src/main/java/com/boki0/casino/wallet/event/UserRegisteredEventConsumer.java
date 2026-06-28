package com.boki0.casino.wallet.event;

import com.boki0.casino.wallet.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredEventConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegisteredEventConsumer.class);

    private final UserRegisteredEventHandler userRegisteredEventHandler;

    public UserRegisteredEventConsumer(UserRegisteredEventHandler userRegisteredEventHandler) {
        this.userRegisteredEventHandler = userRegisteredEventHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_REGISTERED)
    public void consume(UserRegisteredEvent event) {
        LOGGER.info(
                "Received UserRegisteredEvent eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );
        userRegisteredEventHandler.handle(event);
    }
}
