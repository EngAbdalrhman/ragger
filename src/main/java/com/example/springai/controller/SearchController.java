package com.example.springai.controller;

import com.example.springai.search.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/similar")
    public ResponseEntity<?> findSimilarContent(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        try {
            validateApiKey(apiKey);

            String content = (String) request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Content is required"));
            }

            Integer limit = (Integer) request.getOrDefault("limit", 5);
            @SuppressWarnings("unchecked")
            List<String> collections = (List<String>) request.getOrDefault("collections", null);

            Map<String, Object> results = searchService.findSimilarContent(
                    content, limit, collections);

            return ResponseEntity.ok(results);
        } catch (SecurityException e) {
            log.warn("Unauthorized search attempt", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or missing API key"));
        } catch (Exception e) {
            log.error("Error in similar content search", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process search request"));
        }
    }

    @PostMapping("/similar/document/{documentId}")
    public ResponseEntity<?> findSimilarDocuments(
            @PathVariable String documentId,
            @RequestParam(defaultValue = "5") int limit,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        try {
            validateApiKey(apiKey);

            Map<String, Object> results = searchService.findSimilarDocuments(
                    documentId, limit);

            return ResponseEntity.ok(results);
        } catch (SecurityException e) {
            log.warn("Unauthorized search attempt", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or missing API key"));
        } catch (SearchService.DocumentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error finding similar documents", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to find similar documents"));
        }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> batchSimilarSearch(
            @RequestBody List<Map<String, Object>> requests,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        try {
            validateApiKey(apiKey);

            if (requests.size() > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Maximum 10 requests allowed in batch"));
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Map<String, Object> request : requests) {
                String content = (String) request.get("content");
                Integer limit = (Integer) request.getOrDefault("limit", 5);
                @SuppressWarnings("unchecked")
                List<String> collections = (List<String>) request.getOrDefault("collections", null);

                Map<String, Object> result = searchService.findSimilarContent(
                        content, limit, collections);
                results.add(result);
            }

            return ResponseEntity.ok(Map.of(
                    "results", results,
                    "total", results.size()
            ));
        } catch (SecurityException e) {
            log.warn("Unauthorized batch search attempt", e);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid or missing API key"));
        } catch (Exception e) {
            log.error("Error in batch search", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process batch search request"));
        }
    }

    private void validateApiKey(String apiKey) {
        // In a real application, validate the API key against a database or service
        if (apiKey == null || !isValidApiKey(apiKey)) {
            throw new SecurityException("Invalid API key");
        }
    }

    private boolean isValidApiKey(String apiKey) {
        // Implement your API key validation logic
        // For example, check against a list of valid keys or validate with a service
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
