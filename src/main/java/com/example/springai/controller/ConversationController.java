package com.example.springai.controller;

import com.example.springai.conversation.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping("/chat")
    public ResponseEntity<?> processUserInput(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            String input = request.get("message");
            if (input == null || input.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Message is required"));
            }

            // Use default user ID if not provided
            String effectiveUserId = userId != null ? userId : "default-user";

            String response = conversationService.processUserInput(
                    effectiveUserId, input);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("response", response);
            responseBody.put("userId", effectiveUserId);

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("Error processing user input", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process input"));
        }
    }

    @PostMapping("/analyze-document")
    public ResponseEntity<?> analyzeDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is required"));
            }

            // Use default user ID if not provided
            String effectiveUserId = userId != null ? userId : "default-user";

            // Extract text from document and analyze it
            String documentText = extractTextFromDocument(file);
            String response = conversationService.processUserInput(
                    effectiveUserId, documentText);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("response", response);
            responseBody.put("userId", effectiveUserId);
            responseBody.put("fileName", file.getOriginalFilename());

            return ResponseEntity.ok(responseBody);
        } catch (Exception e) {
            log.error("Error analyzing document", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to analyze document"));
        }
    }

    private String extractTextFromDocument(MultipartFile file) {
        // TODO: Implement document text extraction based on file type
        // This could use Apache POI for Office documents, PDFBox for PDFs, etc.
        return "Extracted text from document";
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetConversation(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            String effectiveUserId = userId != null ? userId : "default-user";

            // Reset the conversation state
            conversationService.processUserInput(effectiveUserId, "reset");

            return ResponseEntity.ok(Map.of(
                    "message", "Conversation reset successfully",
                    "userId", effectiveUserId
            ));
        } catch (Exception e) {
            log.error("Error resetting conversation", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reset conversation"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getConversationStatus(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            String effectiveUserId = userId != null ? userId : "default-user";

            // Get the current conversation state
            Map<String, Object> status = new HashMap<>();
            status.put("userId", effectiveUserId);
            status.put("active", true);
            // Add more status information as needed

            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting conversation status", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get conversation status"));
        }
    }
}
