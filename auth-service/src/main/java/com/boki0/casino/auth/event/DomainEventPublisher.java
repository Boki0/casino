package com.boki0.casino.auth.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
