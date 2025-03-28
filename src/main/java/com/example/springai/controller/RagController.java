package com.example.springai.controller;

import com.example.springai.rag.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final BatchProcessingService batchProcessingService;
    private final DocumentCollectionRepository collectionRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String collectionId,
            @RequestParam(required = false) Set<String> tags,
            @RequestParam(required = false, defaultValue = "false") boolean batch,
            @RequestHeader("X-User-Id") String userId) {
        try {
            String documentId;
            if (batch) {
                CompletableFuture<String> future = batchProcessingService.processBatchDocument(file, userId);
                documentId = future.get(); // Wait for completion
            } else {
                documentId = ragService.processAndStoreDocument(file);
            }

            // Update collection if specified
            if (collectionId != null) {
                DocumentCollection collection = collectionRepository.findById(collectionId)
                        .orElseThrow(() -> new RuntimeException("Collection not found"));
                DocumentMetadata metadata = documentMetadataRepository.findById(documentId).get();
                metadata.setCollectionId(collectionId);
                metadata.setTags(tags != null ? tags : new HashSet<>());
                documentMetadataRepository.save(metadata);

                // Update collection stats
                collection.setDocumentCount(collection.getDocumentCount() + 1);
                collectionRepository.save(collection);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("documentId", documentId);
            response.put("message", "Document processed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing document", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to process document");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/collections")
    public ResponseEntity<DocumentCollection> createCollection(
            @RequestBody DocumentCollection collection,
            @RequestHeader("X-User-Id") String userId) {
        collection.setOwnerId(userId);
        collection.setCollectionId(UUID.randomUUID().toString());
        return ResponseEntity.ok(collectionRepository.save(collection));
    }

    @GetMapping("/collections")
    public ResponseEntity<List<DocumentCollection>> getCollections(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) Set<String> roles) {
        return ResponseEntity.ok(collectionRepository.findAccessibleCollections(userId, roles));
    }

    @PostMapping("/documents/{documentId}/versions")
    public ResponseEntity<DocumentVersion> createVersion(
            @PathVariable String documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        try {
            return ResponseEntity.ok(ragService.createNewVersion(documentId, file, description));
        } catch (Exception e) {
            log.error("Error creating version", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/documents/{documentId}/versions")
    public ResponseEntity<List<DocumentVersion>> getVersions(@PathVariable String documentId) {
        return ResponseEntity.ok(versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId));
    }

    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryDocument(
            @RequestParam String query,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String collectionId,
            @RequestParam(required = false) Integer version) {
        try {
            String answer;
            if (collectionId != null) {
                answer = ragService.queryCollection(collectionId, query, modelName);
            } else {
                answer = ragService.queryDocument(query, modelName, version);
            }

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

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable String documentId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            ragService.deleteDocument(documentId, userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Document deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
