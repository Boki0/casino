package com.boki0.casino.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingUserRegisteredEventHandler implements UserRegisteredEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUserRegisteredEventHandler.class);

    @Override
    public void handle(UserRegisteredEvent event) {
        LOGGER.info(
                "Handling UserRegisteredEvent eventId={}, authUserId={}",
                event.eventId(),
                event.authUserId()
        );
    }
}
