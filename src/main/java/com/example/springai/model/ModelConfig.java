package com.example.springai.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.model")
public class ModelConfig {

    private String defaultModel = "local";
    private boolean queueEnabled = true;
    private int maxTokens = 4096;
}
