package com.boki0.casino.payment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "casino.events";
    public static final String ROUTING_KEY_PAYMENT_DEPOSIT_COMPLETED = "payment.deposit.completed";
    public static final String QUEUE_WALLET_DEPOSIT_CREDITED = "payment-service.wallet-deposit-credited.queue";
    public static final String ROUTING_KEY_WALLET_DEPOSIT_CREDITED = "wallet.deposit.credited";

    @Bean
    public TopicExchange casinoEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue walletDepositCreditedQueue() {
        return new Queue(QUEUE_WALLET_DEPOSIT_CREDITED);
    }

    @Bean
    public Binding walletDepositCreditedBinding(
            Queue walletDepositCreditedQueue,
            TopicExchange casinoEventsExchange
    ) {
        return BindingBuilder.bind(walletDepositCreditedQueue)
                .to(casinoEventsExchange)
                .with(ROUTING_KEY_WALLET_DEPOSIT_CREDITED);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);

        return rabbitTemplate;
    }
}
