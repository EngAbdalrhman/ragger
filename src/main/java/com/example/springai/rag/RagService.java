package com.example.springai.rag;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.springai.service.AiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final TextChunkRepository textChunkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentCollectionRepository collectionRepository;
    private final CacheService cacheService;
    private final FileTypeValidator fileTypeValidator;
    private final BatchProcessingService batchProcessingService;

    private static final int SIMILAR_CHUNKS_LIMIT = 3;
    private static final ReentrantLock versionLock = new ReentrantLock();

    @Autowired
    private AiService aiService;

    @Transactional
    @Retryable(value = OptimisticLockingFailureException.class,
            maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String processAndStoreDocument(MultipartFile file) throws IOException {
        // Validate file
        fileTypeValidator.validateFile(file);

        String documentId = java.util.UUID.randomUUID().toString();
        List<TextChunk> chunks = documentProcessor.processDocument(file);

        // Generate embeddings
        embeddingService.generateEmbeddings(chunks);
        chunks.forEach(chunk -> chunk.setDocumentId(documentId));

        // Store chunks
        textChunkRepository.saveAll(chunks);

        // Create metadata
        createDocumentMetadata(file, documentId, chunks.size());

        return documentId;
    }

    @Transactional(readOnly = true)
    public String queryDocument(String query, String modelName, Integer version) {
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        List<TextChunk> similarChunks;
        if (version != null) {
            DocumentVersion docVersion = documentVersionRepository
                    .findByDocumentIdAndVersionNumber(query.split(":")[0], version)
                    .orElseThrow(() -> new IllegalArgumentException("Version not found"));

            similarChunks = textChunkRepository.findSimilarChunksForVersion(
                    queryEmbedding, docVersion.getChunkStartId(),
                    docVersion.getChunkEndId(), SIMILAR_CHUNKS_LIMIT);
        } else {
            similarChunks = textChunkRepository.findSimilarChunks(
                    queryEmbedding, SIMILAR_CHUNKS_LIMIT);
        }

        String context = prepareContext(similarChunks, query);
        return aiService.executeModel(modelName, generatePrompt(context, query));
    }

    @Transactional(readOnly = true)
    public String queryCollection(String collectionId, String query, String modelName) {
        DocumentCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new IllegalArgumentException("Collection not found"));

        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        List<TextChunk> similarChunks = textChunkRepository
                .findSimilarChunksInCollection(queryEmbedding, collectionId, SIMILAR_CHUNKS_LIMIT);

        String context = prepareContext(similarChunks, query);
        return aiService.executeModel(modelName != null ? modelName
                : collection.getDefaultAiModel(), generatePrompt(context, query));
    }

    @Transactional
    public DocumentVersion createNewVersion(String documentId, MultipartFile file,
            String description) throws IOException {
        versionLock.lock();
        try {
            // Validate file
            fileTypeValidator.validateFile(file);

            // Get current version
            Integer currentVersion = documentVersionRepository
                    .findLatestVersionNumber(documentId)
                    .orElse(0);

            // Process new version
            List<TextChunk> newChunks = documentProcessor.processDocument(file);
            embeddingService.generateEmbeddings(newChunks);
            newChunks.forEach(chunk -> chunk.setDocumentId(documentId));

            // Save chunks and get IDs
            List<TextChunk> savedChunks = textChunkRepository.saveAll(newChunks);
            Long startId = savedChunks.get(0).getId();
            Long endId = savedChunks.get(savedChunks.size() - 1).getId();

            // Create version entry
            DocumentVersion version = new DocumentVersion();
            version.setDocumentId(documentId);
            version.setVersionNumber(currentVersion + 1);
            version.setCreatedAt(LocalDateTime.now());
            version.setChunkStartId(startId);
            version.setChunkEndId(endId);
            version.setChangeDescription(description);
            version.setStatus(DocumentVersion.ProcessingStatus.COMPLETED);

            return documentVersionRepository.save(version);
        } finally {
            versionLock.unlock();
        }
    }

    @Transactional
    public void deleteDocument(String documentId, String userId) {
        DocumentMetadata metadata = documentMetadataRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!metadata.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to delete this document");
        }

        // Update collection if needed
        if (metadata.getCollectionId() != null) {
            DocumentCollection collection = collectionRepository
                    .findById(metadata.getCollectionId()).orElse(null);
            if (collection != null) {
                collection.setDocumentCount(collection.getDocumentCount() - 1);
                collectionRepository.save(collection);
            }
        }

        // Delete all related data
        textChunkRepository.deleteByDocumentId(documentId);
        documentVersionRepository.deleteByDocumentId(documentId);
        documentMetadataRepository.deleteById(documentId);

        // Clear cache
        cacheService.evictDocument(documentId);
    }

    private void createDocumentMetadata(MultipartFile file, String documentId,
            int chunkCount) {
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setDocumentId(documentId);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileType(getFileExtension(file.getOriginalFilename()));
        metadata.setChunkCount(chunkCount);
        metadata.setUploadTimestamp(LocalDateTime.now());
        metadata.setFileSize(file.getSize());
        metadata.setMimeType(file.getContentType());

        documentMetadataRepository.save(metadata);
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String prepareContext(List<TextChunk> chunks, String query) {
        return chunks.stream()
                .map(TextChunk::getContent)
                .collect(Collectors.joining("\n\n"));
    }

    private String generatePrompt(String context, String query) {
        return String.format("""
            Use the following context to answer the question.
            If you cannot find the answer in the context, say so.
            
            Context:
            %s
            
            Question:
            %s
            
            Answer:""", context, query);
    }
}
