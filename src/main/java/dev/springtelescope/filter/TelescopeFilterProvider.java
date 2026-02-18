package dev.springtelescope.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Provides filter options for the Telescope dashboard dropdowns.
 * Implement this interface and register as a Spring bean to customize
 * the user and tenant lists shown in the dashboard filters.
 */
public interface TelescopeFilterProvider {

    /**
     * List of users for the dropdown filter. Each map should contain "id" and "name" keys.
     */
    default List<Map<String, String>> getUsers() {
        return Collections.emptyList();
    }

    /**
     * List of tenants for the dropdown filter. Each map should contain "id" and "name" keys.
     */
    default List<Map<String, String>> getTenants() {
        return Collections.emptyList();
    }
}
