package com.example.springai.conversation;

import com.example.springai.messaging.AIMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedConversationService {

    private final AIConversationUtils aiUtils;
    private final AIMessageService messageService;
    private final ObjectMapper objectMapper;
    private final ConversationLogger conversationLogger;

    public String processUserInput(String userId, String input) {
        try {
            // 1. Analyze prompt using AI
            Map<String, Object> analysis = aiUtils.analyzePrompt(input, userId);

            // 2. Queue for processing
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("input", input);
            message.put("analysis", analysis);
            messageService.queueLocalModelRequest(message, 5);

            // 3. Return immediate response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "processing");
            response.put("message", "Your request is being processed");
            response.put("sessionId", userId);

            conversationLogger.logConversation(
                    userId, input, "Request queued", "PROCESSING",
                    Map.of("analysis", analysis), true, null
            );

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error processing input", e);
            conversationLogger.logConversation(
                    userId, input, null, "ERROR",
                    null, false, e.getMessage()
            );
            return "{\"error\":\"processing_failed\"}";
        }
    }

    public String processDocument(String userId, MultipartFile file) {
        try {
            // 1. Create processing message
            Map<String, Object> message = new HashMap<>();
            message.put("userId", userId);
            message.put("filename", file.getOriginalFilename());
            message.put("contentType", file.getContentType());
            message.put("size", file.getSize());

            // 2. Queue for processing
            messageService.queueLocalModelRequest(message, 7); // Higher priority

            // 3. Return response
            Map<String, Object> response = new HashMap<>();
            response.put("status", "processing");
            response.put("message", "Your document is being processed");
            response.put("sessionId", userId);

            conversationLogger.logDocumentAnalysis(
                    userId, file.getOriginalFilename(),
                    message, null, true, null
            );

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error processing document", e);
            return "{\"error\":\"document_processing_failed\"}";
        }
    }
}
