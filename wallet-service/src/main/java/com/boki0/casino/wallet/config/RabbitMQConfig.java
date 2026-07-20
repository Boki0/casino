package com.boki0.casino.wallet.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "casino.events";
    public static final String QUEUE_USER_REGISTERED = "wallet-service.user-registered.queue";
    public static final String QUEUE_PAYMENT_DEPOSIT_COMPLETED = "wallet-service.payment-deposit-completed.queue";
    public static final String ROUTING_KEY_USER_REGISTERED = "user.registered";
    public static final String ROUTING_KEY_PAYMENT_DEPOSIT_COMPLETED = "payment.deposit.completed";

    @Bean
    public TopicExchange casinoEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(QUEUE_USER_REGISTERED);
    }

    @Bean
    public Queue paymentDepositCompletedQueue() {
        return new Queue(QUEUE_PAYMENT_DEPOSIT_COMPLETED);
    }

    @Bean
    public Binding userRegisteredBinding(
            Queue userRegisteredQueue,
            TopicExchange casinoEventsExchange
    ) {
        return BindingBuilder.bind(userRegisteredQueue)
                .to(casinoEventsExchange)
                .with(ROUTING_KEY_USER_REGISTERED);
    }

    @Bean
    public Binding paymentDepositCompletedBinding(
            Queue paymentDepositCompletedQueue,
            TopicExchange casinoEventsExchange
    ) {
        return BindingBuilder.bind(paymentDepositCompletedQueue)
                .to(casinoEventsExchange)
                .with(ROUTING_KEY_PAYMENT_DEPOSIT_COMPLETED);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);

        return factory;
    }
}
