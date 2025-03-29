package com.example.springai.controller;

import com.example.springai.conversation.AIConversationUtils;
import com.example.springai.messaging.AIMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-test")
public class AITestController {
    private final AIConversationUtils aiUtils;
    private final AIMessageService messageService;

    public AITestController(AIConversationUtils aiUtils, AIMessageService messageService) {
        this.aiUtils = aiUtils;
        this.messageService = messageService;
    }

    @PostMapping("/analyze")
    public Map<String, Object> testPromptAnalysis(@RequestBody String prompt) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Test prompt analysis
            Map<String, Object> analysis = aiUtils.analyzePrompt(prompt, "test-session");
            response.put("analysis", analysis);

            // Test message queuing
            Map<String, Object> testMessage = Map.of(
                "prompt", prompt,
                "session", "test-session",
                "timestamp", System.currentTimeMillis()
            );
            messageService.queueLocalModelRequest(testMessage, 5);
            response.put("messageStatus", "queued");

            return response;
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/config")
    public Map<String, String> checkConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put("status", "active");
        config.put("service", "AI Test Endpoint");
        config.put("message", "Configuration verified");
        return config;
    }
}