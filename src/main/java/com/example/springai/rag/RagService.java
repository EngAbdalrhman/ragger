package com.example.springai.rag;

import com.example.springai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final DocumentProcessor documentProcessor;
    private final EmbeddingService embeddingService;
    private final TextChunkRepository textChunkRepository;
    private final AiService aiService;

    private static final int SIMILAR_CHUNKS_LIMIT = 3;

    public String processAndStoreDocument(MultipartFile file) throws IOException {
        String documentId = UUID.randomUUID().toString();
        List<TextChunk> chunks = documentProcessor.processDocument(file);

        // Generate embeddings for each chunk
        embeddingService.generateEmbeddings(chunks);

        // Set document ID for each chunk
        chunks.forEach(chunk -> chunk.setDocumentId(documentId));

        // Store chunks with embeddings
        textChunkRepository.saveAll(chunks);

        return documentId;
    }

    public String queryDocument(String query, String modelName) {
        // Generate embedding for the query
        float[] queryEmbedding = embeddingService.generateEmbedding(query);

        // Find similar chunks
        List<TextChunk> similarChunks = textChunkRepository.findSimilarChunks(
                queryEmbedding, SIMILAR_CHUNKS_LIMIT);

        // Prepare context from similar chunks
        String context = similarChunks.stream()
                .map(TextChunk::getContent)
                .collect(Collectors.joining("\n\n"));

        // Generate prompt with context
        String prompt = String.format("""
                Use the following context to answer the question. If you cannot find the answer in the context, say so.
                
                Context:
                %s
                
                Question:
                %s
                
                Answer:""", context, query);

        // Get response from AI model
        return aiService.executeModel(modelName, prompt);
    }
}
