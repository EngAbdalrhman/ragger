package com.example.springai.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, String> {

    List<DocumentMetadata> findByFileType(String fileType);

    Optional<DocumentMetadata> findByFileName(String fileName);

    List<DocumentMetadata> findAllByOrderByUploadTimestampDesc();
}
