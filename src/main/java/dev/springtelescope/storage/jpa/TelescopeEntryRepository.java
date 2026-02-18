package dev.springtelescope.storage.jpa;

import dev.springtelescope.model.TelescopeEntryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface TelescopeEntryRepository extends JpaRepository<TelescopeEntryEntity, String> {

    @Query("SELECT e FROM TelescopeEntryEntity e WHERE e.type = :type" +
            " AND (:user IS NULL OR e.userIdentifier = :user)" +
            " AND (:tenant IS NULL OR e.tenantId = :tenant)" +
            " ORDER BY e.createdAt DESC")
    List<TelescopeEntryEntity> findByTypeFiltered(
            @Param("type") TelescopeEntryType type,
            @Param("user") String userIdentifier,
            @Param("tenant") String tenantId,
            Pageable pageable);

    @Query("SELECT COUNT(e) FROM TelescopeEntryEntity e WHERE e.type = :type" +
            " AND (:user IS NULL OR e.userIdentifier = :user)" +
            " AND (:tenant IS NULL OR e.tenantId = :tenant)")
    long countByTypeFiltered(
            @Param("type") TelescopeEntryType type,
            @Param("user") String userIdentifier,
            @Param("tenant") String tenantId);

    List<TelescopeEntryEntity> findByBatchIdOrderByCreatedAtAsc(String batchId);

    @Query("SELECT COUNT(e) FROM TelescopeEntryEntity e WHERE e.type = :type")
    long countByType(@Param("type") TelescopeEntryType type);

    @Query("SELECT DISTINCT e.userIdentifier FROM TelescopeEntryEntity e WHERE e.userIdentifier IS NOT NULL ORDER BY e.userIdentifier")
    List<String> findDistinctUserIdentifiers();

    @Query("SELECT DISTINCT e.tenantId FROM TelescopeEntryEntity e WHERE e.tenantId IS NOT NULL ORDER BY e.tenantId")
    List<String> findDistinctTenantIds();

    @Query("SELECT DISTINCT e.tagsJson FROM TelescopeEntryEntity e WHERE e.tagsJson IS NOT NULL")
    List<String> findDistinctTagsJson();

    @Modifying
    @Transactional
    void deleteByType(TelescopeEntryType type);

    @Modifying
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
