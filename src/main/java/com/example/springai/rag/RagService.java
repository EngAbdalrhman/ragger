package com.example.springai.rag;

import com.example.springai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final TextChunkRepository textChunkRepository;
    private final AiService aiService;

    private static final int SIMILAR_CHUNKS_LIMIT = 3;
    private static final int MAX_CONTEXT_LENGTH = 4000;

    @Transactional
    public String processAndStoreDocument(MultipartFile file) throws IOException {
        String documentId = UUID.randomUUID().toString();
        List<TextChunk> chunks = documentProcessor.processDocument(file);

        // Generate embeddings for each chunk
        embeddingService.generateEmbeddings(chunks);

        // Set document ID for each chunk
        chunks.forEach(chunk -> chunk.setDocumentId(documentId));

        // Store chunks with embeddings
        textChunkRepository.saveAll(chunks);

        // Create and store document metadata
        DocumentMetadata metadata = new DocumentMetadata();
        metadata.setDocumentId(documentId);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setFileType(getFileExtension(file.getOriginalFilename()));
        metadata.setChunkCount(chunks.size());
        metadata.setUploadTimestamp(LocalDateTime.now());
        metadata.setTotalTokens(calculateTotalTokens(chunks));

        // Generate a summary using the AI model
        String summary = generateSummary(chunks);
        metadata.setSummary(summary);

        documentMetadataRepository.save(metadata);

        return documentId;
    }

    @Transactional(readOnly = true)
    public String queryDocument(String query, String modelName) {
        // Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // Find similar chunks
        List<TextChunk> similarChunks = textChunkRepository.findSimilarChunks(
                queryEmbedding, SIMILAR_CHUNKS_LIMIT);

        // Prepare context from similar chunks with metadata
        String context = prepareContext(similarChunks, query);

        // Generate prompt with context
        String prompt = generatePrompt(context, query);

        // Get response from AI model
        return aiService.executeModel(modelName, prompt);
    }

    private String prepareContext(List<TextChunk> chunks, String query) {
        StringBuilder context = new StringBuilder();

        // Add relevant document metadata
        if (!chunks.isEmpty()) {
            String documentId = chunks.get(0).getDocumentId();
            documentMetadataRepository.findById(documentId).ifPresent(metadata
                    -> context.append("Document: ")
                            .append(metadata.getFileName())
                            .append("\nSummary: ")
                            .append(metadata.getSummary())
                            .append("\n\nRelevant Sections:\n")
            );
        }

        // Add chunk contents with separators
        chunks.forEach(chunk
                -> context.append("---\n")
                        .append(chunk.getContent())
                        .append("\n")
        );

        return context.toString();
    }

    private String generatePrompt(String context, String query) {
        return String.format("""
                Use the following context to answer the question. 
                If you cannot find the answer in the context, say so.
                Base your answer only on the provided context.
                
                Context:
                %s
                
                Question:
                %s
                
                Answer in a clear and concise way. If you use information from the context,
                explain which part of the context supports your answer.
                
                Answer:""", context, query);
    }

    private String generateSummary(List<TextChunk> chunks) {
        String combinedContent = chunks.stream()
                .map(TextChunk::getContent)
                .limit(3) // Use first 3 chunks for summary
                .collect(Collectors.joining("\n\n"));

        String summaryPrompt = String.format("""
                Generate a brief summary of the following text in 2-3 sentences:
                
                %s
                
                Summary:""", combinedContent);

        return aiService.executeModel(null, summaryPrompt); // Use default model
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private int calculateTotalTokens(List<TextChunk> chunks) {
        // Rough estimation: 1 token ≈ 4 characters
        return chunks.stream()
                .mapToInt(chunk -> chunk.getContent().length() / 4)
                .sum();
    }
}
