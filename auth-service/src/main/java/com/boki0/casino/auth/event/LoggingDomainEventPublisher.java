package com.boki0.casino.auth.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        LOGGER.info(
                "Publishing domain event type={}, version={}, eventId={}",
                event.eventType(),
                event.eventVersion(),
                event.eventId()
        );
    }
}
