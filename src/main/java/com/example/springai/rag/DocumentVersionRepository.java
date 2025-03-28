package com.example.springai.rag;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNumberDesc(String documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNumber(String documentId, Integer versionNumber);

    @Query("SELECT MAX(v.versionNumber) FROM DocumentVersion v WHERE v.documentId = ?1")
    Optional<Integer> findLatestVersionNumber(String documentId);

    List<DocumentVersion> findByStatusOrderByCreatedAtDesc(DocumentVersion.ProcessingStatus status);

    @Query("SELECT v FROM DocumentVersion v WHERE v.documentId = ?1 AND v.isActive = true")
    Optional<DocumentVersion> findActiveVersion(String documentId);

    @Modifying
    @Query("DELETE FROM DocumentVersion v WHERE v.documentId = :documentId")
    void deleteByDocumentId(@Param("documentId") String documentId);
}
