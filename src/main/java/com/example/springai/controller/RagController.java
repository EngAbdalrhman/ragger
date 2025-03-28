package com.example.springai.controller;

import com.example.springai.rag.RagService;
import com.example.springai.rag.DocumentMetadata;
import com.example.springai.rag.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final DocumentMetadataRepository documentMetadataRepository;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        try {
            String documentId = ragService.processAndStoreDocument(file);
            DocumentMetadata metadata = documentMetadataRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Metadata not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("documentId", documentId);
            response.put("fileName", metadata.getFileName());
            response.put("chunkCount", metadata.getChunkCount());
            response.put("summary", metadata.getSummary());
            response.put("message", "Document processed successfully");

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error processing document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process document");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryDocument(
            @RequestParam String query,
            @RequestParam(required = false) String modelName) {
        try {
            String answer = ragService.queryDocument(query, modelName);
            Map<String, Object> response = new HashMap<>();
            response.put("answer", answer);
            response.put("model", modelName != null ? modelName : "default");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error querying document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process query");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentMetadata>> listDocuments() {
        return ResponseEntity.ok(documentMetadataRepository.findAllByOrderByUploadTimestampDesc());
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<DocumentMetadata> getDocumentMetadata(@PathVariable String documentId) {
        return documentMetadataRepository.findById(documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable String documentId) {
        Map<String, String> response = new HashMap<>();
        if (documentMetadataRepository.existsById(documentId)) {
            documentMetadataRepository.deleteById(documentId);
            response.put("message", "Document deleted successfully");
            return ResponseEntity.ok(response);
        }
        response.put("message", "Document not found");
        return ResponseEntity.notFound().build();
    }
}
