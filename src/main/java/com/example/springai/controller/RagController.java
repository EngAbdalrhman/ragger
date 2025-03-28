package com.example.springai.controller;

import com.example.springai.rag.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        try {
            String documentId = ragService.processAndStoreDocument(file);
            Map<String, String> response = new HashMap<>();
            response.put("documentId", documentId);
            response.put("message", "Document processed successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error processing document", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process document: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, String>> queryDocument(
            @RequestParam String query,
            @RequestParam(required = false) String modelName) {
        try {
            String answer = ragService.queryDocument(query, modelName);
            Map<String, String> response = new HashMap<>();
            response.put("answer", answer);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error querying document", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to process query: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
