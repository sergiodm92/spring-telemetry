package dev.springtelescope.storage;

import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TelescopeStorage {

    private final Map<TelescopeEntryType, ConcurrentLinkedDeque<TelescopeEntry>> entries = new ConcurrentHashMap<>();
    private final int maxEntriesPerType;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public TelescopeStorage(int maxEntriesPerType) {
        this.maxEntriesPerType = maxEntriesPerType;
        for (TelescopeEntryType type : TelescopeEntryType.values()) {
            entries.put(type, new ConcurrentLinkedDeque<>());
        }
    }

    public void store(TelescopeEntry entry) {
        if (!enabled.get() || entry == null || entry.getType() == null) return;
        ConcurrentLinkedDeque<TelescopeEntry> deque = entries.get(entry.getType());
        if (deque == null) return;
        deque.addFirst(entry);
        while (deque.size() > maxEntriesPerType) {
            deque.removeLast();
        }
    }

    public List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size) {
        return getByType(type, page, size, null, null, null, null);
    }

    public List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size,
                                          String userIdentifier, String tenantId) {
        return getByType(type, page, size, userIdentifier, tenantId, null, null);
    }

    public List<TelescopeEntry> getByType(TelescopeEntryType type, int page, int size,
                                          String userIdentifier, String tenantId,
                                          String method, String statusGroup) {
        ConcurrentLinkedDeque<TelescopeEntry> deque = entries.get(type);
        if (deque == null) return Collections.emptyList();
        return applyFilters(deque.stream(), userIdentifier, tenantId, method, statusGroup)
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());
    }

    public long countByType(TelescopeEntryType type) {
        return countByType(type, null, null, null, null);
    }

    public long countByType(TelescopeEntryType type, String userIdentifier, String tenantId) {
        return countByType(type, userIdentifier, tenantId, null, null);
    }

    public long countByType(TelescopeEntryType type, String userIdentifier, String tenantId,
                            String method, String statusGroup) {
        ConcurrentLinkedDeque<TelescopeEntry> deque = entries.get(type);
        if (deque == null) return 0;
        return applyFilters(deque.stream(), userIdentifier, tenantId, method, statusGroup).count();
    }

    public Set<String> getDistinctUserIdentifiers() {
        return entries.values().stream()
                .flatMap(Collection::stream)
                .map(TelescopeEntry::getUserIdentifier)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Set<String> getDistinctTenantIds() {
        return entries.values().stream()
                .flatMap(Collection::stream)
                .map(TelescopeEntry::getTenantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private Stream<TelescopeEntry> applyFilters(Stream<TelescopeEntry> stream,
                                                String userIdentifier, String tenantId,
                                                String method, String statusGroup) {
        if (userIdentifier != null && !userIdentifier.isBlank()) {
            stream = stream.filter(e -> userIdentifier.equals(e.getUserIdentifier()));
        }
        if (tenantId != null && !tenantId.isBlank()) {
            stream = stream.filter(e -> tenantId.equals(e.getTenantId()));
        }
        if (method != null && !method.isBlank()) {
            stream = stream.filter(e -> method.equalsIgnoreCase(
                    String.valueOf(e.getContent().get("method"))));
        }
        if (statusGroup != null && !statusGroup.isBlank()) {
            stream = stream.filter(e -> {
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
            });
        }
        return stream;
    }

    public Optional<TelescopeEntry> getByUuid(String uuid) {
        return entries.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getUuid().equals(uuid))
                .findFirst();
    }

    public List<TelescopeEntry> getByBatchId(String batchId) {
        if (batchId == null) return Collections.emptyList();
        return entries.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> batchId.equals(e.getBatchId()))
                .sorted(Comparator.comparing(TelescopeEntry::getCreatedAt))
                .collect(Collectors.toList());
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (TelescopeEntryType type : TelescopeEntryType.values()) {
            stats.put(type.name(), (long) entries.get(type).size());
        }
        return stats;
    }

    public void clear() {
        entries.values().forEach(ConcurrentLinkedDeque::clear);
    }

    public void clearByType(TelescopeEntryType type) {
        ConcurrentLinkedDeque<TelescopeEntry> deque = entries.get(type);
        if (deque != null) deque.clear();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public long pruneOlderThan(LocalDateTime cutoff) {
        long total = 0;
        for (ConcurrentLinkedDeque<TelescopeEntry> deque : entries.values()) {
            long before = deque.size();
            deque.removeIf(e -> e.getCreatedAt() != null && e.getCreatedAt().isBefore(cutoff));
            total += before - deque.size();
        }
        return total;
    }

    public Set<String> getDistinctTags() {
        return entries.values().stream()
                .flatMap(Collection::stream)
                .filter(e -> e.getTags() != null)
                .flatMap(e -> e.getTags().stream())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
