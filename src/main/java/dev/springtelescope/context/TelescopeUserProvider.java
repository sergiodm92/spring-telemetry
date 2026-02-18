package dev.springtelescope.context;

/**
 * Provides the current user's identity for Telescope entries.
 * Implement this interface and register as a Spring bean to customize
 * how Telescope identifies users and tenants.
 */
public interface TelescopeUserProvider {

    /**
     * Returns the current user's identifier (email, username, or ID).
     * @return the user identifier, or null if no session is active.
     */
    String getCurrentUserIdentifier();

    /**
     * Returns the current tenant/organization ID (optional).
     * @return the tenant ID, or null if not applicable.
     */
    default String getCurrentTenantId() {
        return null;
    }
}
