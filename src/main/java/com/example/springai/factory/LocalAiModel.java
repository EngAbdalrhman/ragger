package com.example.springai.factory;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LocalAiModel implements AiModel {
    @Override
    public String execute(String input) {
        log.info("Processing input with Local model: {}", input);
        // Here you would implement your custom local model logic
        return "Local AI processed: " + input;
    }
}