package com.example.springai.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.request}")
    private String requestQueue;

    @Value("${rabbitmq.queue.response}")
    private String responseQueue;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.request}")
    private String requestRoutingKey;

    @Value("${rabbitmq.routing-key.response}")
    private String responseRoutingKey;

    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueue, true);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(responseQueue, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding requestBinding(Queue requestQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(requestQueue)
                .to(exchange)
                .with(requestRoutingKey);
    }

    @Bean
    public Binding responseBinding(Queue responseQueue, TopicExchange exchange) {
        return BindingBuilder
                .bind(responseQueue)
                .to(exchange)
                .with(responseRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
