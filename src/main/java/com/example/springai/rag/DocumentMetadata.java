package com.example.springai.rag;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    // Collection support
    @Column(name = "collection_id")
    private String collectionId;

    @Column(name = "tags")
    @ElementCollection
    private Set<String> tags = new HashSet<>();

    // Access control
    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @ElementCollection
    @CollectionTable(name = "document_access_users",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "user_id")
    private Set<String> accessUsers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "document_access_roles",
            joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "role")
    private Set<String> accessRoles = new HashSet<>();

    // Version tracking
    @Column(name = "current_version")
    private Integer currentVersion = 1;

    @Column(name = "is_archived")
    private boolean isArchived = false;

    // Batch processing status
    @Column(name = "processing_status")
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    public enum ProcessingStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    // Cache control
    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "access_count")
    private Long accessCount = 0L;

    // Additional file type support
    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "page_count")
    private Integer pageCount;

    // Metadata
    @Column(columnDefinition = "jsonb")
    private String additionalMetadata;
}
