package com.example.springai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchProcessingService {

    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final TextChunkRepository textChunkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentVersionRepository documentVersionRepository;

    private static final int BATCH_SIZE = 50;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Async
    public CompletableFuture<String> processBatchDocument(MultipartFile file, String userId) throws IOException {
        String documentId = java.util.UUID.randomUUID().toString();
        DocumentVersion version = createInitialVersion(documentId);

        try {
            // Process document in chunks
            List<TextChunk> allChunks = documentProcessor.processDocument(file);
            int totalChunks = allChunks.size();

            // Update version with total chunks
            version.setStatus(DocumentVersion.ProcessingStatus.PROCESSING);
            documentVersionRepository.save(version);

            // Process chunks in batches
            List<CompletableFuture<Void>> batchFutures = new ArrayList<>();
            for (int i = 0; i < totalChunks; i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, totalChunks);
                List<TextChunk> batch = allChunks.subList(i, end);

                CompletableFuture<Void> batchFuture = CompletableFuture.runAsync(()
                        -> processBatch(batch, documentId), executorService);
                batchFutures.add(batchFuture);
            }

            // Wait for all batches to complete
            CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

            // Create document metadata
            createDocumentMetadata(file, documentId, userId, totalChunks);

            // Update version status
            version.setStatus(DocumentVersion.ProcessingStatus.COMPLETED);
            documentVersionRepository.save(version);

            log.info("Batch processing completed for document: {}", documentId);
            return CompletableFuture.completedFuture(documentId);

        } catch (Exception e) {
            log.error("Error in batch processing for document: {}", documentId, e);
            version.setStatus(DocumentVersion.ProcessingStatus.FAILED);
            version.setChangeDescription("Failed: " + e.getMessage());
            documentVersionRepository.save(version);
            throw e;
        }
    }

    @Transactional
    protected void processBatch(List<TextChunk> chunks, String documentId) {
        try {
            // Generate embeddings for the batch
            embeddingService.generateEmbeddings(chunks);

            // Set document ID for each chunk
            chunks.forEach(chunk -> chunk.setDocumentId(documentId));

            // Save batch to database
            textChunkRepository.saveAll(chunks);

            log.debug("Processed batch of {} chunks for document {}", chunks.size(), documentId);
        } catch (Exception e) {
            log.error("Error processing batch for document {}", documentId, e);
            throw new RuntimeException("Batch processing failed", e);
        }
    }

    private DocumentVersion createInitialVersion(String documentId) {
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setVersionNumber(1);
        version.setCreatedAt(java.time.LocalDateTime.now());
        version.setStatus(DocumentVersion.ProcessingStatus.PENDING);
        return documentVersionRepository.save(version);
    }

    private void createDocumentMetadata(MultipartFile file, String documentId, String userId, int totalChunks) {
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setDocumentId(documentId);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileType(getFileExtension(file.getOriginalFilename()));
        metadata.setChunkCount(totalChunks);
        metadata.setUploadTimestamp(java.time.LocalDateTime.now());
        metadata.setOwnerId(userId);
        metadata.setFileSize(file.getSize());
        metadata.setMimeType(file.getContentType());
        metadata.setProcessingStatus(DocumentMetadata.ProcessingStatus.COMPLETED);

        documentMetadataRepository.save(metadata);
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
