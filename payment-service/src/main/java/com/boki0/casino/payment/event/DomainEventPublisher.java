package com.boki0.casino.payment.event;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
