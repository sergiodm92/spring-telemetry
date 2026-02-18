package dev.springtelescope.filter;

import dev.springtelescope.storage.TelescopeStorage;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Default implementation that extracts distinct values from the storage.
 * No database dependency required.
 */
@RequiredArgsConstructor
public class DefaultTelescopeFilterProvider implements TelescopeFilterProvider {

    private final TelescopeStorage storage;

    @Override
    public List<Map<String, String>> getUsers() {
        return storage.getDistinctUserIdentifiers().stream()
                .map(id -> Map.of("id", id, "name", id))
                .toList();
    }

    @Override
    public List<Map<String, String>> getTenants() {
        return storage.getDistinctTenantIds().stream()
                .map(id -> Map.of("id", id, "name", id))
                .toList();
    }
}
