package com.example.springai.messaging;

import com.example.springai.conversation.AIConversationUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Handles AI message queuing with RabbitMQ - Manages local model message queue
 * - Implements retry mechanisms - Processes responses
 */
@Service
public class AIMessageService {

    private final RabbitTemplate rabbitTemplate;
    private final AIConversationUtils conversationUtils;
    private static final String LOCAL_QUEUE = "ai.local.requests";
    private static final String DLQ = "ai.local.requests.dlq";

    public AIMessageService(RabbitTemplate rabbitTemplate,
            AIConversationUtils conversationUtils) {
        this.rabbitTemplate = rabbitTemplate;
        this.conversationUtils = conversationUtils;
    }

    /**
     * Queues message for local model processing
     *
     * @param payload Message content
     * @param priority Message priority (0-9)
     */
    @Retryable(
            value = {AmqpException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void queueLocalModelRequest(Map<String, Object> payload, int priority) {
        String message = conversationUtils.formatForQueue(payload);

        rabbitTemplate.convertAndSend(LOCAL_QUEUE, (Object) message, (MessagePostProcessor) msg -> {
            msg.getMessageProperties().setPriority(priority);
            msg.getMessageProperties().setHeader("x-retries", 0);
            msg.getMessageProperties().setHeader("x-dead-letter-routing-key", DLQ);
            return msg;
        });

    }

    /**
     * Processes failed messages (dead letter handler)
     *
     * @param failedMessage The failed message
     */
    public void handleFailedMessage(Message failedMessage) {
        int retries = failedMessage.getMessageProperties()
                .getHeader("x-retries") != null
                ? (int) failedMessage.getMessageProperties().getHeader("x-retries") : 0;

        if (retries < 3) {
            failedMessage.getMessageProperties()
                    .setHeader("x-retries", retries + 1);
            rabbitTemplate.send(LOCAL_QUEUE, failedMessage);
        } else {
            rabbitTemplate.send(DLQ, failedMessage);
        }
    }
}
