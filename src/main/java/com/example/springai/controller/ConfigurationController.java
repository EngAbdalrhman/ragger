package com.example.springai.controller;

import com.example.springai.entity.ApiKey;
import com.example.springai.entity.ModelConfig;
import com.example.springai.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configurationService;

    @PostMapping("/api-keys")
    public ResponseEntity<ApiKey> addApiKey(@RequestBody ApiKey apiKey) {
        log.info("Adding new API key for provider: {}", apiKey.getProvider());
        return ResponseEntity.ok(configurationService.saveApiKey(apiKey));
    }

    @PostMapping("/models")
    public ResponseEntity<ModelConfig> addModelConfig(@RequestBody ModelConfig modelConfig) {
        log.info("Adding new model configuration for provider: {}", modelConfig.getProvider());
        return ResponseEntity.ok(configurationService.saveModelConfig(modelConfig));
    }

    @GetMapping("/models")
    public ResponseEntity<List<ModelConfig>> getActiveModels() {
        return ResponseEntity.ok(configurationService.getAllActiveModels());
    }

    @GetMapping("/models/default")
    public ResponseEntity<ModelConfig> getDefaultModel() {
        return ResponseEntity.ok(configurationService.getDefaultModelConfig());
    }

    @GetMapping("/models/{provider}")
    public ResponseEntity<ModelConfig> getModelConfig(@PathVariable String provider) {
        return ResponseEntity.ok(configurationService.getModelConfig(provider));
    }
}
