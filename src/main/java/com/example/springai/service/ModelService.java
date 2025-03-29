package com.example.springai.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.springai.model.CustomModelProvider;
import com.example.springai.model.LocalModelProvider;
import com.example.springai.model.ModelConfigEntity;
import com.example.springai.model.ModelProvider;
import com.example.springai.model.RestModelProvider;
import com.example.springai.token.TokenCalculator;

@Service
public class ModelService {

    private final Map<String, ModelProvider> providers = new HashMap<>();
    private final LicenseService licenseService;
    private final TokenService tokenService;
    private final TokenCalculator tokenCalculator;

    @Autowired
    public ModelService(LocalModelProvider localProvider,
            RestModelProvider restProvider,
            LicenseService licenseService,
            TokenService tokenService,
            TokenCalculator tokenCalculator) {
        this.licenseService = licenseService;
        this.tokenService = tokenService;
        this.tokenCalculator = tokenCalculator;

        // Register providers
        providers.put(localProvider.getProviderName(), localProvider);
        providers.put(restProvider.getProviderName(), restProvider);
    }

    public Map<String, String> listModels() {
        Map<String, String> models = new HashMap<>();
        for (Map.Entry<String, ModelProvider> entry : providers.entrySet()) {
            models.put(entry.getKey(), entry.getValue().getClass().getSimpleName());
        }
        return models;
    }

    public void addModel(ModelConfigEntity config) {
        // Implement logic to add a new model configuration
        // This could involve saving the configuration to a database or updating an in-memory structure
        // Example:
        providers.put(config.getModelName(), new CustomModelProvider(config));
    }

    public int calculateTokens(String text) {
        return tokenCalculator.calculateTokens(text);
    }

    public String processRequest(String licenseKey, String modelType, String input) {
        if (!licenseService.validateLicense(licenseKey)) {
            return "Invalid license key";
        }

        int tokens = tokenService.calculateTokens(input);
        if (!licenseService.canMakeRequest(licenseKey, input)) {
            return "Insufficient tokens. Required: " + tokens;
        }

        ModelProvider provider = providers.getOrDefault(modelType, providers.get("local"));
        licenseService.deductTokens(licenseKey, tokens);

        return provider.processRequest(input);
    }

    public int getRemainingTokens(String licenseKey) {
        return licenseService.getRemainingTokens(licenseKey);
    }
}
