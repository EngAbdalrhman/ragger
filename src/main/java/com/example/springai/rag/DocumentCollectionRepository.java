package com.example.springai.rag;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DocumentCollectionRepository extends JpaRepository<DocumentCollection, String> {

    List<DocumentCollection> findByOwnerId(String ownerId);

    List<DocumentCollection> findByIsPublicTrue();

    @Query("SELECT c FROM DocumentCollection c WHERE c.ownerId = :userId "
            + "OR :userId MEMBER OF c.accessUsers "
            + "OR EXISTS (SELECT r FROM c.accessRoles r WHERE r IN :userRoles)")
    List<DocumentCollection> findAccessibleCollections(
            @Param("userId") String userId,
            @Param("userRoles") Set<String> userRoles);

    List<DocumentCollection> findByParentCollectionId(String parentCollectionId);

    @Query("SELECT c FROM DocumentCollection c WHERE "
            + ":tag MEMBER OF c.tags")
    List<DocumentCollection> findByTag(@Param("tag") String tag);

    @Query("SELECT c FROM DocumentCollection c WHERE "
            + "LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR "
            + "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<DocumentCollection> searchCollections(@Param("searchTerm") String searchTerm);

    @Query("SELECT COUNT(c) FROM DocumentCollection c WHERE "
            + "c.parentCollectionId = :collectionId")
    long countSubCollections(@Param("collectionId") String collectionId);

    @Query("SELECT c FROM DocumentCollection c WHERE "
            + "c.documentCount > 0 AND "
            + "c.updatedAt = (SELECT MAX(c2.updatedAt) FROM DocumentCollection c2)")
    Optional<DocumentCollection> findMostRecentlyUpdatedNonEmptyCollection();

    @Query(value = "WITH RECURSIVE collection_tree AS ("
            + "  SELECT collection_id, parent_collection_id, 0 as level "
            + "  FROM document_collections "
            + "  WHERE collection_id = :rootId "
            + "  UNION ALL "
            + "  SELECT c.collection_id, c.parent_collection_id, ct.level + 1 "
            + "  FROM document_collections c "
            + "  INNER JOIN collection_tree ct ON c.parent_collection_id = ct.collection_id"
            + ") "
            + "SELECT * FROM collection_tree", nativeQuery = true)
    List<Object[]> getCollectionHierarchy(@Param("rootId") String rootId);
}
