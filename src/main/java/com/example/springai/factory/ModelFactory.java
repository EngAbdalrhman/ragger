package com.example.springai.factory;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.example.springai.exception.ModelNotFoundException;
import com.example.springai.service.ConfigurationService;
import com.example.springai.entity.ModelConfig;
import org.springframework.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelFactory {

    private final ApplicationContext applicationContext;
    private final ConfigurationService configurationService;

    public AiModel getAiModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            // Get default model configuration
            ModelConfig defaultConfig = configurationService.getDefaultModelConfig();
            log.info("Using default model: {}", defaultConfig.getProvider());
            return createModel(defaultConfig.getProvider());
        }

        String provider = modelName.toLowerCase().trim();
        try {
            // Get model configuration for the specified provider
            ModelConfig config = configurationService.getModelConfig(provider);
            log.info("Creating model for provider: {} with model name: {}",
                    config.getProvider(), config.getModelName());
            return createModel(config.getProvider());
        } catch (Exception e) {
            log.error("Error creating model for provider: {}", provider, e);
            throw new ModelNotFoundException("Model not found or configuration error: " + provider);
        }
    }

    private AiModel createModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" ->
                applicationContext.getBean(OpenAiModel.class);
            case "gemini" ->
                applicationContext.getBean(GeminiModel.class);
            case "local" ->
                applicationContext.getBean(LocalAiModel.class);
            default ->
                throw new ModelNotFoundException("Model not found: " + provider);
        };
    }
}
