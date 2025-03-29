package com.example.springai.factory;

import org.springframework.stereotype.Component;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import com.example.springai.service.ConfigurationService;
import com.example.springai.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiModel implements AiModel {

    private final ChatClient chatClient;
    private final ConfigurationService configurationService;

    @Override
    public String generateContent(String input) {
        ModelConfig config = configurationService.getModelConfig("openai");
        log.info("Processing input with OpenAI model: {} ({})", config.getModelName(), input);

        // Use the configuration from database
        System.setProperty("spring.ai.openai.api-key",
                configurationService.getApiKey("openai"));
        System.setProperty("spring.ai.openai.model",
                config.getModelName());
        if (config.getTemperature() != null) {
            System.setProperty("spring.ai.openai.temperature",
                    config.getTemperature().toString());
        }

        return chatClient.call(new Prompt(input))
                .getResult().getOutput().getContent();
    }
}
