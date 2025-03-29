package com.example.springai.controller;

import com.example.springai.conversation.EnhancedConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/conversation")
@Tag(name = "AI Conversation API", description = "Version 2 of AI conversation endpoints with enhanced features")
public class DocumentedConversationController {

    private final EnhancedConversationService conversationService;

    public DocumentedConversationController(EnhancedConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(
            summary = "Process text input",
            description = "Analyzes text using AI and queues for processing",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Success response with processing status",
                        content = @Content(
                                mediaType = "application/json",
                                examples = @ExampleObject(
                                        value = """
                        {
                          "status": "processing",
                          "message": "Your request is being processed",
                          "sessionId": "user123"
                        }
                        """
                                )
                        )
                )
            }
    )
    @PostMapping("/text")
    public String processText(
            @Parameter(description = "User ID", required = true, example = "user123")
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Text input to analyze", required = true,
                    example = "Create a customer management app")
            @RequestBody String input) {
        return conversationService.processUserInput(userId, input);
    }

    @Operation(
            summary = "Process document",
            description = "Uploads document for AI analysis",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Document accepted for processing",
                        content = @Content(
                                examples = @ExampleObject(
                                        value = """
                        {
                          "status": "processing", 
                          "message": "Document received",
                          "sessionId": "user123"
                        }
                        """
                                )
                        )
                )
            }
    )
    @PostMapping(value = "/document", consumes = "multipart/form-data")
    public String processDocument(
            @Parameter(description = "User ID", required = true, example = "user123")
            @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "Document file to upload", required = true)
            @RequestPart("file") MultipartFile file) {
        return conversationService.processDocument(userId, file);
    }

    @Operation(
            summary = "Service health check",
            description = "Verifies the service is operational"
    )
    @GetMapping("/health")
    public String healthCheck() {
        return "{\"status\":\"operational\"}";
    }
}
