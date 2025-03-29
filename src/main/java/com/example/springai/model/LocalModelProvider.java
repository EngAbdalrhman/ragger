package com.example.springai.model;

import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.example.springai.token.TokenCalculator;

@Component
public class LocalModelProvider implements ModelProvider {

    private final RabbitTemplate rabbitTemplate;
    private final TokenCalculator tokenCalculator;

    public LocalModelProvider(RabbitTemplate rabbitTemplate, TokenCalculator tokenCalculator) {
        this.rabbitTemplate = rabbitTemplate;
        this.tokenCalculator = tokenCalculator;
    }

    @Override
    public String getProviderName() {
        return "local";
    }

    @Override
    public String processRequest(String input) {
        //TODO  Implement the logic for processing the request with the local model
        return "Processed with local model: " + input;
    }

    @Override
    public String generateContent(String prompt) {
        //TODO  Implement the logic for generating content  
        rabbitTemplate.convertAndSend("ai.local.queue", Map.of(
                "prompt", prompt,
                "timestamp", System.currentTimeMillis()
        ));
        return "{\"status\":\"queued\",\"message\":\"Processing via local model\"}";
    }

    @Override
    public int calculateTokens(String text) {
        return tokenCalculator.calculateTokens(text);
    }
}
