package com.example.springai.controller;

import com.example.springai.dto.AiRequest;
import com.example.springai.dto.AiResponse;
import com.example.springai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @PostMapping("/execute")
    public ResponseEntity<AiResponse> executeModel(@RequestBody AiRequest request) {
        log.info("Received request for model: {} with input: {}", request.getModelName(), request.getInput());
        String result = aiService.executeModel(request.getModelName(), request.getInput());
        return ResponseEntity.ok(new AiResponse(result, request.getModelName()));
    }

    @GetMapping("/models")
    public ResponseEntity<String[]> getAvailableModels() {
        // Return list of available models
        return ResponseEntity.ok(new String[]{"openai", "gemini", "local"});
    }
}