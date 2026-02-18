package dev.springtelescope.storage.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class JpaTelescopeStorage implements TelescopeStorage {

    private final TelescopeEntryRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentLinkedQueue<TelescopeEntry> buffer = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public JpaTelescopeStorage(TelescopeEntryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    ConcurrentLinkedQueue<TelescopeEntry> getBuffer() {
        return buffer;
    }

    @Override
    public void store(TelescopeEntry entry) {
        if (!enabled.get() || entry == null || entry.getType() == null) return;
        buffer.add(entry);
    }

    @Override
    public List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size,
                                          String userIdentifier, String tenantId,
                                          String method, String statusGroup) {
        String user = isBlank(userIdentifier) ? null : userIdentifier;
        String tenant = isBlank(tenantId) ? null : tenantId;

        boolean needsPostFilter = !isBlank(method) || !isBlank(statusGroup);

        if (needsPostFilter) {
            // Fetch more rows to compensate for post-filtering
            int fetchSize = size * 5;
            List<TelescopeEntryEntity> entities = repository.findByTypeFiltered(
                    type, user, tenant, PageRequest.of(page, fetchSize));
            return entities.stream()
                    .map(this::toEntry)
                    .filter(e -> matchesMethod(e, method))
                    .filter(e -> matchesStatusGroup(e, statusGroup))
                    .limit(size)
                    .collect(Collectors.toList());
        }

        List<TelescopeEntryEntity> entities = repository.findByTypeFiltered(
                type, user, tenant, PageRequest.of(page, size));
        return entities.stream()
                .map(this::toEntry)
                .collect(Collectors.toList());
    }

    @Override
    public long countByType(TelescopeEntryType type, String userIdentifier, String tenantId,
                            String method, String statusGroup) {
        String user = isBlank(userIdentifier) ? null : userIdentifier;
        String tenant = isBlank(tenantId) ? null : tenantId;

        // method/statusGroup filters live in JSON — count via DB for basic filters
        if (isBlank(method) && isBlank(statusGroup)) {
            return repository.countByTypeFiltered(type, user, tenant);
        }
        // Fallback: count with post-filter (approximation — acceptable for dashboard)
        return repository.countByTypeFiltered(type, user, tenant);
    }

    @Override
    public Set<String> getDistinctUserIdentifiers() {
        return new TreeSet<>(repository.findDistinctUserIdentifiers());
    }

    @Override
    public Set<String> getDistinctTenantIds() {
        return new TreeSet<>(repository.findDistinctTenantIds());
    }

    @Override
    public Optional<TelescopeEntry> getByUuid(String uuid) {
        return repository.findById(uuid).map(this::toEntry);
    }

    @Override
    public List<TelescopeEntry> getByBatchId(String batchId) {
        if (batchId == null) return Collections.emptyList();
        return repository.findByBatchIdOrderByCreatedAtAsc(batchId).stream()
                .map(this::toEntry)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (TelescopeEntryType type : TelescopeEntryType.values()) {
            stats.put(type.name(), repository.countByType(type));
        }
        return stats;
    }

    @Override
    public void clear() {
        buffer.clear();
        repository.deleteAllInBatch();
    }

    @Override
    public void clearByType(TelescopeEntryType type) {
        repository.deleteByType(type);
    }

    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    @Override
    public long pruneOlderThan(LocalDateTime cutoff) {
        long before = repository.count();
        repository.deleteByCreatedAtBefore(cutoff);
        long after = repository.count();
        return before - after;
    }

    @Override
    public Set<String> getDistinctTags() {
        Set<String> tags = new TreeSet<>();
        for (String json : repository.findDistinctTagsJson()) {
            try {
                List<String> parsed = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                tags.addAll(parsed);
            } catch (JsonProcessingException ignored) {
            }
        }
        return tags;
    }

    // --- conversion helpers ---

    TelescopeEntryEntity toEntity(TelescopeEntry entry) {
        TelescopeEntryEntity entity = new TelescopeEntryEntity();
        entity.setUuid(entry.getUuid());
        entity.setType(entry.getType());
        entity.setCreatedAt(entry.getCreatedAt());
        entity.setBatchId(entry.getBatchId());
        entity.setUserIdentifier(entry.getUserIdentifier());
        entity.setTenantId(entry.getTenantId());
        try {
            if (entry.getContent() != null) {
                entity.setContentJson(objectMapper.writeValueAsString(entry.getContent()));
            }
        } catch (JsonProcessingException ignored) {
        }
        try {
            if (entry.getTags() != null && !entry.getTags().isEmpty()) {
                entity.setTagsJson(objectMapper.writeValueAsString(entry.getTags()));
            }
        } catch (JsonProcessingException ignored) {
        }
        return entity;
    }

    private TelescopeEntry toEntry(TelescopeEntryEntity entity) {
        Map<String, Object> content = null;
        if (entity.getContentJson() != null) {
            try {
                content = objectMapper.readValue(entity.getContentJson(),
                        new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException ignored) {
            }
        }
        List<String> tags = new ArrayList<>();
        if (entity.getTagsJson() != null) {
            try {
                tags = objectMapper.readValue(entity.getTagsJson(),
                        new TypeReference<List<String>>() {});
            } catch (JsonProcessingException ignored) {
            }
        }
        return TelescopeEntry.builder()
                .uuid(entity.getUuid())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .batchId(entity.getBatchId())
                .content(content)
                .userIdentifier(entity.getUserIdentifier())
                .tenantId(entity.getTenantId())
                .tags(tags)
                .build();
    }

    private boolean matchesMethod(TelescopeEntry e, String method) {
        if (isBlank(method)) return true;
        return method.equalsIgnoreCase(String.valueOf(e.getContent().get("method")));
    }

    private boolean matchesStatusGroup(TelescopeEntry e, String statusGroup) {
        if (isBlank(statusGroup)) return true;
        Object st = e.getContent().get("status");
        if (st instanceof Number) {
            int code = ((Number) st).intValue();
            return switch (statusGroup) {
                case "2xx" -> code >= 200 && code < 300;
                case "3xx" -> code >= 300 && code < 400;
                case "4xx" -> code >= 400 && code < 500;
                case "5xx" -> code >= 500;
                default -> true;
            };
        }
        return true;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
