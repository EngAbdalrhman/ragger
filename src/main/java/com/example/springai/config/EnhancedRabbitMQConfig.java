package com.example.springai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

@Configuration
@EnableConfigurationProperties(AIConfigProperties.class)
@Primary
public class EnhancedRabbitMQConfig {

    private final AIConfigProperties aiConfig;

    public EnhancedRabbitMQConfig(AIConfigProperties aiConfig) {
        this.aiConfig = aiConfig;
    }

    @Bean
    public Queue aiRequestQueue() {
        return QueueBuilder.durable("ai.local.requests")
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", "ai.local.requests.dlq")
                .maxPriority(10)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue("ai.local.requests.dlq", true);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(aiConfig.getMaxRetries())
                .backOffOptions(
                        aiConfig.getRetryDelay(),
                        2.0,
                        aiConfig.getTimeout()
                )
                .build();
    }

    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(
                "localhost",
                5672
        );
        factory.setChannelCacheSize(25);
        factory.setConnectionCacheSize(5);
        return factory;
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setMessageConverter(messageConverter());
        template.setChannelTransacted(true);
        template.setReplyTimeout(aiConfig.getTimeout());
        return template;
    }
}
