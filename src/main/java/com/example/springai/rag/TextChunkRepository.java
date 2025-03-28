package com.example.springai.rag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TextChunkRepository extends JpaRepository<TextChunk, Long> {

    List<TextChunk> findByDocumentId(String documentId);

    @Query(value = "SELECT * FROM text_chunks "
            + "ORDER BY embedding <-> CAST(:queryEmbedding AS float[]) "
            + "LIMIT :limit", nativeQuery = true)
    List<TextChunk> findSimilarChunks(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("limit") int limit);

    @Query(value = "SELECT * FROM text_chunks WHERE document_id IN "
            + "(SELECT document_id FROM document_collections WHERE collection_id = :collectionId) "
            + "ORDER BY embedding <-> CAST(:queryEmbedding AS float[]) "
            + "LIMIT :limit", nativeQuery = true)
    List<TextChunk> findSimilarChunksInCollection(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("collectionId") String collectionId,
            @Param("limit") int limit);

    List<TextChunk> findSimilarChunksForVersion(
            @Param("queryEmbedding") float[] queryEmbedding,
            @Param("chunkStartId") Long chunkStartId,
            @Param("chunkEndId") Long chunkEndId,
            @Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM TextChunk t WHERE t.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);
}
