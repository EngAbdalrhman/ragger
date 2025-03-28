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
@Table(name = "document_collections")
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCollection {

    @Id
    @Column(name = "collection_id")
    private String collectionId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @ElementCollection
    @CollectionTable(name = "collection_access_users",
            joinColumns = @JoinColumn(name = "collection_id"))
    @Column(name = "user_id")
    private Set<String> accessUsers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "collection_access_roles",
            joinColumns = @JoinColumn(name = "collection_id"))
    @Column(name = "role")
    private Set<String> accessRoles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "collection_tags",
            joinColumns = @JoinColumn(name = "collection_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @Column(name = "parent_collection_id")
    private String parentCollectionId;

    @Column(name = "is_public")
    private boolean isPublic = false;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "total_tokens")
    private Long totalTokens = 0L;

    @Column(name = "embedding_model")
    private String embeddingModel;

    @Column(name = "default_ai_model")
    private String defaultAiModel;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
