package com.example.springai.rag;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "document_metadata")
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {

    @Id
    @Column(name = "document_id")
    private String documentId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "upload_timestamp", nullable = false)
    private LocalDateTime uploadTimestamp;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(columnDefinition = "TEXT")
    private String summary;
}
