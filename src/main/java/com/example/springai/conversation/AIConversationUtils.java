package com.example.springai.conversation;

import com.example.springai.factory.AiModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced conversation utilities for AI interactions - Handles prompt analysis
 * - Manages response generation - Formats messages for queues
 */
@Component
public class AIConversationUtils {

    private final ObjectMapper objectMapper;
    private final AiModel aiModel;

    public AIConversationUtils(ObjectMapper objectMapper, AiModel aiModel) {
        this.objectMapper = objectMapper;
        this.aiModel = aiModel;
    }

    /**
     * Analyzes user prompt to determine intent and operation type
     *
     * @param userPrompt The input from user
     * @param sessionId Current conversation session
     * @return Map containing operation type and analysis results
     */
    public Map<String, Object> analyzePrompt(String userPrompt, String sessionId) {
        String compositePrompt = """
            Analyze the user request and classify the intent:
            Operations:
            - "create": For new item requests
            - "modify": For changes to existing items
            - "explain": For information requests  
            - "build": For deployment/execution
            - "privileges": For access control changes
            
            Response format:
            {
                "operation": "detected_operation",
                "message": "detailed_response",
                "entities": ["relevant_entities"]
            }
            
            User prompt: %s
            """.formatted(userPrompt);

        String response = aiModel.generateContent(compositePrompt);
        return parseResponse(response);
    }

    /**
     * Generates context-aware responses
     *
     * @param prompt User input
     * @param context Current conversation context
     * @return Generated response
     */
    public String generateResponse(String prompt, Map<String, Object> context) {
        try {
            String contextJson = objectMapper.writeValueAsString(context);
            String enhancedPrompt = """
                Context: %s
                Task: %s
                Requirements:
                - Respond in JSON format
                - Include 'message' with detailed response
                - Suggest next steps in 'actions'
                - List relevant entities
                """.formatted(contextJson, prompt);

            return aiModel.generateContent(enhancedPrompt);
        } catch (JsonProcessingException e) {
            return """
                {
                    "error": "Failed to process context",
                    "message": "Please try again"
                }
                """;
        }
    }

    /**
     * Formats messages for RabbitMQ queue
     *
     * @param payload Content to send
     * @return JSON string ready for queue
     */
    public String formatForQueue(Map<String, Object> payload) {
        Map<String, Object> message = new HashMap<>();
        message.put("metadata", Map.of(
                "timestamp", System.currentTimeMillis(),
                "version", "1.0"
        ));
        message.put("payload", payload);
        message.put("retry_policy", Map.of(
                "max_attempts", 3,
                "current_attempt", 0
        ));

        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization_failed\"}";
        }
    }

    private Map<String, Object> parseResponse(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of(
                    "operation", "error",
                    "message", "Failed to parse AI response"
            );
        }
    }

    /**
     * Enhanced version with template support
     *
     * @param template Template map
     * @param instructions Modification instructions
     * @return Modified template JSON
     */
    public String modifyTemplate(Map<String, Object> template, String instructions) {
        try {
            String templateJson = objectMapper.writeValueAsString(template);
            String prompt = """
                Modify the following JSON template according to instructions:
                Template: %s
                Instructions: %s
                Rules:
                - Preserve all existing fields
                - Only modify specified fields
                - Return complete JSON
                """.formatted(templateJson, instructions);

            return aiModel.generateContent(prompt);
        } catch (JsonProcessingException e) {
            return template.toString();
        }
    }
}
