package com.example.springai.service;

import com.example.springai.factory.ModelFactory;
import com.example.springai.factory.AiModel;
import com.example.springai.entity.ModelConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ModelFactory modelFactory;
    private final ConfigurationService configurationService;

    public String executeModel(String modelName, String input) {
        String selectedModel = modelName;

        if (modelName == null || modelName.trim().isEmpty()) {
            // Use default model if none specified
            ModelConfig defaultConfig = configurationService.getDefaultModelConfig();
            selectedModel = defaultConfig.getProvider();
            log.info("No model specified, using default model: {}", selectedModel);
        }

        log.info("Executing AI model: {} with input: {}", selectedModel, input);
        AiModel model = modelFactory.getAiModel(selectedModel);
        return model.generateContent(input);
    }
}
