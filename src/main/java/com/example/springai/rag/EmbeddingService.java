package com.example.springai.rag;

import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService() {
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    public float[] generateEmbedding(String text) {
        try {
            return embeddingModel.embed(text).content().vector();
        } catch (Exception e) {
            log.error("Error generating embedding for text", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    public void generateEmbeddings(List<TextChunk> chunks) {
        for (TextChunk chunk : chunks) {
            chunk.setEmbedding(generateEmbedding(chunk.getContent()));
        }
    }
}
