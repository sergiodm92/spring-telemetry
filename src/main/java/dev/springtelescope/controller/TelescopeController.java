package dev.springtelescope.controller;

import dev.springtelescope.TelescopeApiResponse;
import dev.springtelescope.filter.TelescopeFilterProvider;
import dev.springtelescope.model.TelescopeEntry;
import dev.springtelescope.model.TelescopeEntryType;
import dev.springtelescope.storage.TelescopeStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${telescope.base-path:/telescope}/api")
@RequiredArgsConstructor
public class TelescopeController {

    private final TelescopeStorage storage;
    private final TelescopeFilterProvider filterProvider;

    @GetMapping("/entries")
    public ResponseEntity<TelescopeApiResponse<Map<String, Object>>> getEntries(
            @RequestParam TelescopeEntryType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String userIdentifier,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String statusGroup) {

        List<TelescopeEntry> entries = storage.getByType(type, page, size, userIdentifier, tenantId, method, statusGroup);

        if (search != null && !search.isBlank()) {
            String lowerSearch = search.toLowerCase();
            entries = entries.stream()
                    .filter(e -> e.getContent().values().stream()
                            .anyMatch(v -> v != null && v.toString().toLowerCase().contains(lowerSearch)))
                    .collect(Collectors.toList());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("entries", entries);
        result.put("total", storage.countByType(type, userIdentifier, tenantId, method, statusGroup));
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(TelescopeApiResponse.success("Entries retrieved", result));
    }

    @GetMapping("/filters")
    public ResponseEntity<TelescopeApiResponse<Map<String, Object>>> getFilters() {
        List<Map<String, String>> users = filterProvider.getUsers();
        List<Map<String, String>> tenants = filterProvider.getTenants();

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("users", users);
        filters.put("tenants", tenants);
        filters.put("methods", List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        filters.put("statuses", List.of("2xx", "3xx", "4xx", "5xx"));
        return ResponseEntity.ok(TelescopeApiResponse.success("Filters", filters));
    }

    @GetMapping("/entries/{uuid}")
    public ResponseEntity<TelescopeApiResponse<TelescopeEntry>> getEntry(@PathVariable String uuid) {
        return storage.getByUuid(uuid)
                .map(e -> ResponseEntity.ok(TelescopeApiResponse.success("Entry retrieved", e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/entries/{uuid}/related")
    public ResponseEntity<TelescopeApiResponse<List<TelescopeEntry>>> getRelatedEntries(@PathVariable String uuid) {
        return storage.getByUuid(uuid)
                .map(e -> ResponseEntity.ok(TelescopeApiResponse.success("Related entries", storage.getByBatchId(e.getBatchId()))))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<TelescopeApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(TelescopeApiResponse.success("Statistics", storage.getStats()));
    }

    @DeleteMapping("/entries")
    public ResponseEntity<TelescopeApiResponse<String>> clearEntries(
            @RequestParam(required = false) TelescopeEntryType type) {
        if (type != null) {
            storage.clearByType(type);
        } else {
            storage.clear();
        }
        return ResponseEntity.ok(TelescopeApiResponse.success("Entries cleared"));
    }

    @PostMapping("/toggle")
    public ResponseEntity<TelescopeApiResponse<Boolean>> toggle() {
        storage.setEnabled(!storage.isEnabled());
        return ResponseEntity.ok(TelescopeApiResponse.success(
                "Telescope " + (storage.isEnabled() ? "enabled" : "disabled"),
                storage.isEnabled()));
    }

    @GetMapping("/status")
    public ResponseEntity<TelescopeApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", storage.isEnabled());
        status.put("stats", storage.getStats());
        return ResponseEntity.ok(TelescopeApiResponse.success("Status", status));
    }

    @PostMapping("/prune")
    public ResponseEntity<TelescopeApiResponse<Map<String, Object>>> prune(
            @RequestParam(defaultValue = "24") int hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        long pruned = storage.pruneOlderThan(cutoff);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pruned", pruned);
        result.put("hours", hours);
        return ResponseEntity.ok(TelescopeApiResponse.success("Pruned " + pruned + " entries", result));
    }

    @GetMapping("/tags")
    public ResponseEntity<TelescopeApiResponse<Set<String>>> getTags() {
        return ResponseEntity.ok(TelescopeApiResponse.success("Tags", storage.getDistinctTags()));
    }
}
