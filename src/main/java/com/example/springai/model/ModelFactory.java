package com.example.springai.model;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;
import javax.annotation.PostConstruct;

@Component
public class ModelFactory {

    private final Map<String, ModelProvider> providers = new HashMap<>();
    private final LocalModelProvider localProvider;
    private final RestModelProvider restProvider;
    private final ModelConfig config;

    public ModelFactory(LocalModelProvider localProvider,
            RestModelProvider restProvider,
            ModelConfig config) {
        this.localProvider = localProvider;
        this.restProvider = restProvider;
        this.config = config;
    }

    @PostConstruct
    public void init() {
        // Register all available providers
        providers.put(localProvider.getProviderName(), localProvider);
        providers.put(restProvider.getProviderName(), restProvider);
    }

    public ModelProvider getProvider(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return providers.get(config.getDefaultModel());
        }
        return providers.getOrDefault(modelName, localProvider);
    }

    public Map<String, String> getAvailableModels() {
        Map<String, String> models = new HashMap<>();
        providers.forEach((name, provider) -> {
            models.put(name, provider.getClass().getSimpleName());
        });
        return models;
    }
}
