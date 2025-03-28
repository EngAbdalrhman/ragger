package com.example.springai.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResult;
import com.example.springai.rag.DocumentMetadata;
import com.example.springai.rag.TextChunk;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.ArrayList;

import com.meilisearch.sdk.exceptions.MeilisearchException;

@Slf4j
@Service
public class MeilisearchService {

    @Value("${meilisearch.host}")
    private String host;

    @Value("${meilisearch.api-key}")
    private String apiKey;

    private Client client;
    private Index documentIndex;
    private Index chunkIndex;

    private static final String DOCUMENTS_INDEX = "documents";
    private static final String CHUNKS_INDEX = "chunks";

    @PostConstruct
    public void init() throws MeilisearchException {
        client = new Client(new Config(host, apiKey));
        documentIndex = client.index(DOCUMENTS_INDEX);
        chunkIndex = client.index(CHUNKS_INDEX);
    }

    public void indexDocument(DocumentMetadata document) {
        try {
            String documentJson = String.format(
                    "{\"id\":\"%s\",\"fileName\":\"%s\",\"fileType\":\"%s\",\"summary\":\"%s\","
                    + "\"tags\":%s,\"collectionId\":\"%s\",\"ownerId\":\"%s\","
                    + "\"uploadTimestamp\":\"%s\",\"isArchived\":%b,\"additionalMetadata\":\"%s\"}",
                    document.getDocumentId(),
                    document.getFileName(),
                    document.getFileType(),
                    document.getSummary(),
                    toJsonArray(new ArrayList<>(document.getTags())),
                    document.getCollectionId(),
                    document.getOwnerId(),
                    document.getUploadTimestamp().toString(),
                    document.isArchived(),
                    document.getAdditionalMetadata()
            );

            documentIndex.addDocuments(documentJson);
        } catch (Exception e) {
            log.error("Error indexing document: {}", document.getDocumentId(), e);
            throw new SearchIndexingException("Failed to index document", e);
        }
    }

    public void indexChunk(TextChunk chunk) {
        try {
            String chunkJson = String.format(
                    "{\"id\":\"%s\",\"documentId\":\"%s\",\"content\":\"%s\"}",
                    chunk.getId().toString(),
                    chunk.getDocumentId(),
                    chunk.getContent()
            );

            chunkIndex.addDocuments(chunkJson);
        } catch (Exception e) {
            log.error("Error indexing chunk: {}", chunk.getId(), e);
            throw new SearchIndexingException("Failed to index chunk", e);
        }
    }

    public SearchResult searchDocuments(String query, Map<String, List<String>> filters, int limit) {
        try {
            SearchRequest request = new SearchRequest(query).setLimit(limit);

            if (filters != null && !filters.isEmpty()) {
                String filterStr = filters.entrySet().stream()
                        .map(entry -> entry.getValue().stream()
                        .map(value -> entry.getKey() + " = '" + value + "'")
                        .collect(Collectors.joining(" OR ")))
                        .collect(Collectors.joining(" AND "));
                request.setFilter(new String[]{filterStr});
            }

            return (SearchResult) documentIndex.search(request);
        } catch (Exception e) {
            log.error("Error searching documents: {}", query, e);
            throw new SearchException("Failed to search documents", e);
        }
    }

    public SearchResult searchChunks(String query, String documentId, int limit) {
        try {
            SearchRequest request = new SearchRequest(query).setLimit(limit);

            if (documentId != null) {
                request.setFilter(new String[]{"documentId = '" + documentId + "'"});
            }

            return (SearchResult) chunkIndex.search(request);
        } catch (Exception e) {
            log.error("Error searching chunks: {}", query, e);
            throw new SearchException("Failed to search chunks", e);
        }
    }

    public void deleteDocument(String documentId) {
        try {
            documentIndex.deleteDocument(documentId);
        } catch (Exception e) {
            log.error("Error deleting document: {}", documentId, e);
            throw new SearchIndexingException("Failed to delete document", e);
        }
    }

    public void deleteChunk(Long chunkId) {
        try {
            chunkIndex.deleteDocument(chunkId.toString());
        } catch (Exception e) {
            log.error("Error deleting chunk: {}", chunkId, e);
            throw new SearchIndexingException("Failed to delete chunk", e);
        }
    }

    public static class SearchException extends RuntimeException {

        public SearchException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class SearchIndexingException extends RuntimeException {

        public SearchIndexingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private String toJsonArray(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        return "[\"" + String.join("\",\"", list) + "\"]";
    }
}
