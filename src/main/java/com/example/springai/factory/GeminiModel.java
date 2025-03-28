package com.example.springai.factory;

import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertex.VertexAiChatClient;
import com.example.springai.service.ConfigurationService;
import com.example.springai.entity.ModelConfig;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiModel implements AiModel {

    private final VertexAiChatClient vertexAiChatClient;
    private final ConfigurationService configurationService;

    @Override
    public String execute(String input) {
        ModelConfig config = configurationService.getModelConfig("gemini");
        log.info("Processing input with Gemini model: {} ({})", config.getModelName(), input);

        // Use the configuration from database
        System.setProperty("spring.ai.vertex.ai.project-id",
                configurationService.getApiKey("gemini"));
        System.setProperty("spring.ai.vertex.ai.model",
                config.getModelName());
        if (config.getTemperature() != null) {
            System.setProperty("spring.ai.vertex.ai.temperature",
                    config.getTemperature().toString());
        }

        return vertexAiChatClient.call(new Prompt(input))
                .getResult().getOutput().getContent();
    }
}
