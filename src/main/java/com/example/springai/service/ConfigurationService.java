package com.example.springai.service;

import com.example.springai.entity.ApiKey;
import com.example.springai.entity.ModelConfig;
import com.example.springai.repository.ApiKeyRepository;
import com.example.springai.repository.ModelConfigRepository;
import com.example.springai.exception.ModelNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationService {

    private final ApiKeyRepository apiKeyRepository;
    private final ModelConfigRepository modelConfigRepository;

    @Transactional(readOnly = true)
    public String getApiKey(String provider) {
        return apiKeyRepository.findByProviderAndIsActiveTrue(provider)
                .map(ApiKey::getApiKey)
                .orElseThrow(() -> new ModelNotFoundException("No active API key found for provider: " + provider));
    }

    @Transactional(readOnly = true)
    public ModelConfig getModelConfig(String provider) {
        return modelConfigRepository.findByProviderAndIsActiveTrue(provider)
                .orElseGet(this::getDefaultModelConfig);
    }

    @Transactional(readOnly = true)
    public ModelConfig getDefaultModelConfig() {
        return modelConfigRepository.findByIsDefaultTrue()
                .orElseGet(() -> {
                    // Return local model config as ultimate fallback
                    ModelConfig localConfig = new ModelConfig();
                    localConfig.setProvider("local");
                    localConfig.setModelName("local");
                    localConfig.setDefault(true);
                    localConfig.setActive(true);
                    return localConfig;
                });
    }

    @Transactional(readOnly = true)
    public List<ModelConfig> getAllActiveModels() {
        return modelConfigRepository.findAllByIsActiveTrue();
    }

    @Transactional
    public ApiKey saveApiKey(ApiKey apiKey) {
        log.info("Saving API key for provider: {}", apiKey.getProvider());
        return apiKeyRepository.save(apiKey);
    }

    @Transactional
    public ModelConfig saveModelConfig(ModelConfig modelConfig) {
        log.info("Saving model configuration for provider: {} and model: {}",
                modelConfig.getProvider(), modelConfig.getModelName());

        if (modelConfig.isDefault()) {
            // If this config is set as default, remove default flag from other configs
            modelConfigRepository.findByIsDefaultTrue()
                    .ifPresent(existing -> {
                        existing.setDefault(false);
                        modelConfigRepository.save(existing);
                    });
        }

        return modelConfigRepository.save(modelConfig);
    }
}
