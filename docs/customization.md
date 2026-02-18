# Customization Guide

Spring Telescope is designed to be extensible. You can replace core components with your own implementations by simply declaring Spring beans.

## Extension Points

| Interface | Default Implementation | Purpose |
|-----------|----------------------|---------|
| `TelescopeUserProvider` | `DefaultTelescopeUserProvider` | Identifies the current user and tenant |
| `TelescopeFilterProvider` | `DefaultTelescopeFilterProvider` | Provides data for dashboard filter dropdowns |
| `TelescopeStorage` | In-memory implementation | Stores all telescope entries |

All defaults use `@ConditionalOnMissingBean`, so your custom bean always takes precedence.

---

## Custom User Provider

### Why customize?

The default provider uses Spring Security's `SecurityContextHolder` to get the current username. You may want to customize this if:

- You use a custom authentication mechanism (not Spring Security)
- You want to use a different identifier (email vs username vs user ID)
- You need to extract tenant information from JWT claims, request headers, or a custom context
- Your `UserDetails` implementation has additional fields you want to use

### Implementation

```java
import dev.springtelescope.context.TelescopeUserProvider;
import org.springframework.stereotype.Component;

@Component
public class MyUserProvider implements TelescopeUserProvider {

    private final AuthenticationService authService;

    public MyUserProvider(AuthenticationService authService) {
        this.authService = authService;
    }

    @Override
    public String getCurrentUserIdentifier() {
        try {
            User user = authService.getCurrentUser();
            if (user != null) {
                return user.getEmail(); // or user.getId().toString()
            }
        } catch (Exception e) {
            // Return null if no user context is available
        }
        return null;
    }

    @Override
    public String getCurrentTenantId() {
        try {
            User user = authService.getCurrentUser();
            if (user != null && user.getOrganization() != null) {
                return String.valueOf(user.getOrganization().getId());
            }
        } catch (Exception e) {
            // Return null if no tenant context is available
        }
        return null;
    }
}
```

### Important notes

- Return `null` when there is no user session (not an empty string)
- The provider is called from multiple contexts (HTTP requests, scheduled tasks, async threads). Make sure it handles missing request contexts gracefully
- Wrap your logic in try-catch — if the provider throws, it could affect Telescope watchers
- The `getCurrentTenantId()` method has a default implementation that returns `null`, so you only need to override it if you use multi-tenancy

### JWT Example

```java
@Component
public class JwtTelescopeUserProvider implements TelescopeUserProvider {

    @Override
    public String getCurrentUserIdentifier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return null;
    }

    @Override
    public String getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("org_id");
        }
        return null;
    }
}
```

### Request Header Example

```java
@Component
public class HeaderTelescopeUserProvider implements TelescopeUserProvider {

    @Override
    public String getCurrentUserIdentifier() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("X-User-Id");
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public String getCurrentTenantId() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                return attrs.getRequest().getHeader("X-Tenant-Id");
            }
        } catch (Exception ignored) {}
        return null;
    }
}
```

---

## Custom Filter Provider

### Why customize?

The default filter provider extracts distinct user identifiers and tenant IDs from the entries currently in storage. This means:

- It only shows users/tenants that have recorded entries
- It displays raw identifiers (e.g., "user@example.com") without friendly names
- It doesn't require database access

You may want to customize this to:

- Show all users/tenants from your database (not just those with entries)
- Display friendly names alongside identifiers
- Add custom sorting or filtering logic

### Implementation

```java
import dev.springtelescope.filter.TelescopeFilterProvider;
import org.springframework.stereotype.Component;

@Component
public class MyFilterProvider implements TelescopeFilterProvider {

    private final UserRepository userRepository;
    private final OrganizationRepository orgRepository;

    public MyFilterProvider(UserRepository userRepository,
                            OrganizationRepository orgRepository) {
        this.userRepository = userRepository;
        this.orgRepository = orgRepository;
    }

    @Override
    public List<Map<String, String>> getUsers() {
        return userRepository.findAll().stream()
            .map(user -> Map.of(
                "id", user.getEmail(),      // Must match the userIdentifier from TelescopeUserProvider
                "name", user.getFirstName() + " " + user.getLastName()
            ))
            .sorted(Comparator.comparing(m -> m.get("name")))
            .toList();
    }

    @Override
    public List<Map<String, String>> getTenants() {
        return orgRepository.findAll().stream()
            .map(org -> Map.of(
                "id", String.valueOf(org.getId()),  // Must match the tenantId from TelescopeUserProvider
                "name", org.getName()
            ))
            .sorted(Comparator.comparing(m -> m.get("name")))
            .toList();
    }
}
```

### Map format

Each map in the list **must** contain exactly two keys:

| Key | Purpose |
|-----|---------|
| `"id"` | The value used for filtering (must match the identifiers set by `TelescopeUserProvider`) |
| `"name"` | The display name shown in the dropdown |

### Caching

If your user/tenant queries are expensive, consider caching the results:

```java
@Component
public class CachedFilterProvider implements TelescopeFilterProvider {

    private final UserRepository userRepo;
    private List<Map<String, String>> cachedUsers;
    private long lastRefresh = 0;

    @Override
    public List<Map<String, String>> getUsers() {
        if (System.currentTimeMillis() - lastRefresh > 60000) { // Refresh every minute
            cachedUsers = userRepo.findAll().stream()
                .map(u -> Map.of("id", u.getEmail(), "name", u.getFullName()))
                .toList();
            lastRefresh = System.currentTimeMillis();
        }
        return cachedUsers;
    }
}
```

---

## Custom Storage

### Why customize?

The default `TelescopeStorage` uses `ConcurrentLinkedDeque` per entry type, all held in memory. You might want to replace this with:

- A persistent storage (database, Redis)
- A distributed storage for multi-instance deployments
- A storage with different eviction policies

### Implementation

To provide your own storage, declare a `TelescopeStorage` bean. Since `TelescopeStorage` is a concrete class (not an interface), you would need to extend it or provide a custom bean:

```java
@Configuration
public class MyTelescopeConfig {

    @Bean
    public TelescopeStorage telescopeStorage() {
        return new TelescopeStorage(5000); // Custom max entries
    }
}
```

---

## Disabling Specific Watchers

You can disable individual watchers via configuration:

```yaml
telescope:
  watchers:
    requests: true
    queries: false    # Don't capture SQL queries
    logs: false       # Don't capture logs
    mail: false       # Don't capture mail
```

Or programmatically by defining your own beans — since all watcher beans use `@ConditionalOnProperty`, setting a property to `false` prevents the bean from being created.

---

## Adding Custom Tags

The `TelescopeEntry` model supports a `tags` field (list of strings). Some watchers automatically add tags:

- **Cache:** `cache:hit`, `cache:miss`, `cache:evict`, `cache:put`, `cache:{cacheName}`
- **Events:** `event:{eventName}`
- **Models:** `model:created`, `model:updated`, `model:deleted`, `entity:{entityName}`
- **Mail:** `mail`, `to:{recipient}`

You can use the tags API endpoint to retrieve all distinct tags:

```bash
curl http://localhost:8080/telescope/api/tags
```
