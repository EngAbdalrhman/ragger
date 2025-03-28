package com.example.springai.search;

import com.example.springai.rag.TextChunkRepository;
import com.example.springai.rag.DocumentMetadataRepository;
import com.example.springai.rag.EmbeddingService;
import com.example.springai.rag.TextChunk;
import com.example.springai.rag.DocumentMetadata;
import com.meilisearch.sdk.model.SearchResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private final MeilisearchService meilisearchService;
    private final TextChunkRepository textChunkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public Map<String, Object> findSimilarContent(String content, Integer limit, List<String> collections) {
        try {
            Map<String, List<String>> filters = new HashMap<>();
            if (collections != null && !collections.isEmpty()) {
                filters.put("collectionId", collections);
            }

            CompletableFuture<SearchResult> meilisearchFuture = CompletableFuture.supplyAsync(()
                    -> meilisearchService.searchDocuments(content, filters, limit));

            CompletableFuture<List<TextChunk>> vectorFuture = CompletableFuture.supplyAsync(() -> {
                float[] embedding = embeddingService.generateEmbedding(content);
                return textChunkRepository.findSimilarChunks(embedding, limit);
            });

            CompletableFuture.allOf(meilisearchFuture, vectorFuture).join();

            return combineAndRankResults(meilisearchFuture.get(), vectorFuture.get());
        } catch (Exception e) {
            log.error("Error finding similar content", e);
            throw new SearchException("Failed to find similar content", e);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> findSimilarDocuments(String documentId, int limit) {
        try {
            return documentMetadataRepository.findById(documentId)
                    .map(document -> {
                        CompletableFuture<SearchResult> meilisearchFuture = CompletableFuture.supplyAsync(()
                                -> meilisearchService.searchDocuments(document.getSummary(), null, limit));

                        CompletableFuture<List<TextChunk>> vectorFuture = CompletableFuture.supplyAsync(() -> {
                            List<TextChunk> documentChunks = textChunkRepository.findByDocumentId(documentId);
                            if (!documentChunks.isEmpty()) {
                                return textChunkRepository.findSimilarChunks(documentChunks.get(0).getEmbedding(), limit);
                            }
                            return Collections.emptyList();
                        });

                        CompletableFuture.allOf(meilisearchFuture, vectorFuture).join();

                        try {
                            Map<String, Object> result = combineAndRankResults(meilisearchFuture.get(), vectorFuture.get());
                            return result;
                        } catch (Exception e) {
                            return null;
                        }

                    })
                    .orElseThrow(() -> new DocumentNotFoundException("Document not found: " + documentId));
        } catch (Exception e) {
            log.error("Error finding similar documents", e);
            throw new SearchException("Failed to find similar documents", e);
        }
    }

    private Map<String, Object> combineAndRankResults(SearchResult meilisearchResults, List<TextChunk> vectorResults) {
        List<Map<String, Object>> combinedResults = new ArrayList<>();
        Set<String> seenDocuments = new HashSet<>();

        for (Map<String, Object> hit : meilisearchResults.getHits()) {
            String documentId = (String) hit.get("documentId");
            if (!seenDocuments.contains(documentId)) {
                Map<String, Object> result = new HashMap<>();
                result.put("documentId", documentId);
                result.put("score", hit.get("_score"));
                result.put("source", "meilisearch");
                result.put("metadata", hit);
                result.put("rank", calculateRank(hit.get("_score"), "meilisearch"));
                combinedResults.add(result);
                seenDocuments.add(documentId);
            }
        }

        for (TextChunk chunk : vectorResults) {
            String documentId = chunk.getDocumentId();
            if (!seenDocuments.contains(documentId)) {
                documentMetadataRepository.findById(documentId).ifPresent(metadata -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("documentId", documentId);
                    result.put("score", calculateVectorScore(chunk));
                    result.put("source", "vector");
                    result.put("metadata", convertMetadataToMap(metadata));
                    result.put("rank", calculateRank(calculateVectorScore(chunk), "vector"));
                    combinedResults.add(result);
                    seenDocuments.add(documentId);
                });
            }
        }

        combinedResults.sort((a, b) -> Double.compare(((Number) b.get("rank")).doubleValue(),
                ((Number) a.get("rank")).doubleValue()));

        return Map.of(
                "results", combinedResults,
                "total", combinedResults.size(),
                "meilisearchTotal", meilisearchResults.getHits().size(),
                "vectorTotal", vectorResults.size()
        );
    }

    private double calculateVectorScore(TextChunk chunk) {
        return 1.0 - (chunk.getEmbedding()[0] * 0.1);
    }

    private double calculateRank(Object score, String source) {
        double numericScore = ((Number) score).doubleValue();
        return "meilisearch".equals(source) ? numericScore * 1.2 : numericScore;
    }

    private Map<String, Object> convertMetadataToMap(DocumentMetadata metadata) {
        return Map.of(
                "fileName", metadata.getFileName(),
                "fileType", metadata.getFileType(),
                "summary", metadata.getSummary(),
                "uploadTimestamp", metadata.getUploadTimestamp().toString(),
                "tags", metadata.getTags(),
                "collectionId", metadata.getCollectionId()
        );
    }

    public static class SearchException extends RuntimeException {

        public SearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DocumentNotFoundException extends RuntimeException {

        public DocumentNotFoundException(String message) {
            super(message);
        }
    }
}
