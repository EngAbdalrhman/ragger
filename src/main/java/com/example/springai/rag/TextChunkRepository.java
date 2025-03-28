package com.example.springai.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {

    List<TextChunk> findByDocumentId(String documentId);

    @Query(value = "SELECT * FROM text_chunks "
            + "ORDER BY embedding <-> CAST(:queryEmbedding AS float[]) "
            + "LIMIT :limit", nativeQuery = true)
    List<TextChunk> findSimilarChunks(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit);
}
