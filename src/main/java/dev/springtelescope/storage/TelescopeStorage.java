package dev.springtelescope.storage;

import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface TelescopeStorage {

    void store(TelescopeEntry entry);

    List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size,
                                   String userIdentifier, String tenantId,
                                   String method, String statusGroup);

    default List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size) {
        return getByType(type, page, size, null, null, null, null);
    }

    default List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size,
                                           String userIdentifier, String tenantId) {
        return getByType(type, page, size, userIdentifier, tenantId, null, null);
    }

    long countByType(TelescopeEntryType type, String userIdentifier, String tenantId,
                     String method, String statusGroup);

    default long countByType(TelescopeEntryType type) {
        return countByType(type, null, null, null, null);
    }

    default long countByType(TelescopeEntryType type, String userIdentifier, String tenantId) {
        return countByType(type, userIdentifier, tenantId, null, null);
    }

    Set<String> getDistinctUserIdentifiers();

    Set<String> getDistinctTenantIds();

    Optional<TelescopeEntry> getByUuid(String uuid);

    List<TelescopeEntry> getByBatchId(String batchId);

    Map<String, Long> getStats();

    void clear();

    void clearByType(TelescopeEntryType type);

    boolean isEnabled();

    void setEnabled(boolean enabled);

    long pruneOlderThan(LocalDateTime cutoff);

    Set<String> getDistinctTags();
}
