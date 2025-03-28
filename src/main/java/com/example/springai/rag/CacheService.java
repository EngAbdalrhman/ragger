package com.example.springai.rag;

import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final TextChunkRepository textChunkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;

    // In-memory cache for quick access to frequently used chunks
    private final Map<String, CacheEntry<TextChunk>> chunkCache = new ConcurrentHashMap<>();
    private final Map<String, CacheEntry<DocumentMetadata>> metadataCache = new ConcurrentHashMap<>();

    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_EXPIRY_MINUTES = 30;

    @Cacheable(value = "chunks", key = "#documentId + '-' + #chunkIndex")
    public TextChunk getChunk(String documentId, int chunkIndex) {
        String cacheKey = documentId + "-" + chunkIndex;
        CacheEntry<TextChunk> entry = chunkCache.get(cacheKey);

        if (entry != null && !isExpired(entry)) {
            log.debug("Cache hit for chunk: {}", cacheKey);
            return entry.getData();
        }

        log.debug("Cache miss for chunk: {}", cacheKey);
        List<TextChunk> chunks = textChunkRepository.findByDocumentId(documentId);
        if (chunks.size() > chunkIndex) {
            TextChunk chunk = chunks.get(chunkIndex);
            cacheChunk(cacheKey, chunk);
            return chunk;
        }

        return null;
    }

    @Cacheable(value = "documents", key = "#documentId")
    public DocumentMetadata getDocumentMetadata(String documentId) {
        CacheEntry<DocumentMetadata> entry = metadataCache.get(documentId);

        if (entry != null && !isExpired(entry)) {
            log.debug("Cache hit for document metadata: {}", documentId);
            return entry.getData();
        }

        log.debug("Cache miss for document metadata: {}", documentId);
        return documentMetadataRepository.findById(documentId)
                .map(metadata -> {
                    cacheMetadata(documentId, metadata);
                    return metadata;
                })
                .orElse(null);
    }

    @CachePut(value = "chunks", key = "#cacheKey")
    public void cacheChunk(String cacheKey, TextChunk chunk) {
        if (chunkCache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntry(chunkCache);
        }
        chunkCache.put(cacheKey, new CacheEntry<>(chunk));
    }

    @CachePut(value = "documents", key = "#documentId")
    public void cacheMetadata(String documentId, DocumentMetadata metadata) {
        if (metadataCache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntry(metadataCache);
        }
        metadataCache.put(documentId, new CacheEntry<>(metadata));
    }

    @CacheEvict(value = {"chunks", "documents"}, allEntries = true)
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanCache() {
        log.debug("Starting cache cleanup");
        chunkCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
        metadataCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    private <T> void evictOldestEntry(Map<String, CacheEntry<T>> cache) {
        cache.entrySet().stream()
                .min((e1, e2) -> e1.getValue().getTimestamp().compareTo(e2.getValue().getTimestamp()))
                .ifPresent(entry -> cache.remove(entry.getKey()));
    }

    private boolean isExpired(CacheEntry<?> entry) {
        return entry.getTimestamp()
                .plusMinutes(CACHE_EXPIRY_MINUTES)
                .isBefore(LocalDateTime.now());
    }

    private static class CacheEntry<T> {

        private final T data;
        private final LocalDateTime timestamp;

        public CacheEntry(T data) {
            this.data = data;
            this.timestamp = LocalDateTime.now();
        }

        public T getData() {
            return data;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
