package com.example.springai.factory;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import com.example.springai.exception.ModelNotFoundException;
import org.springframework.context.ApplicationContext;

@Component
@RequiredArgsConstructor
public class ModelFactory {
    private final ApplicationContext applicationContext;

    public AiModel getAiModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            throw new ModelNotFoundException("Model name cannot be empty");
        }

        return switch (modelName.toLowerCase().trim()) {
            case "openai" -> applicationContext.getBean(OpenAiModel.class);
            case "gemini" -> applicationContext.getBean(GeminiModel.class);
            case "local" -> applicationContext.getBean(LocalAiModel.class);
            default -> throw new ModelNotFoundException("Model not found: " + modelName);
        };
    }
}