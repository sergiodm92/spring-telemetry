# Configuration Reference

All Telescope configuration is managed through Spring Boot's `@ConfigurationProperties` mechanism under the `telescope` prefix.

## Full Configuration Example

```yaml
telescope:
  enabled: true
  base-package: com.mycompany.myapp
  max-entries: 1000
  prune-hours: 24
  prune-interval-ms: 3600000
  base-path: /telescope
  ignored-prefixes:
    - /actuator
    - /swagger
    - /v3/api-docs
    - /health
  tenant-pattern: "/organizations/(\\d+)"
  watchers:
    requests: true
    queries: true
    exceptions: true
    logs: true
    schedules: true
    cache: true
    events: true
    mail: true
    models: true
```

## Properties

### Core Properties

#### `telescope.enabled`

- **Type:** `boolean`
- **Default:** `true`
- **Description:** Master switch for the entire Telescope library. When set to `false`, the auto-configuration is completely skipped — no beans are created, no watchers are registered, and no memory is consumed.

```yaml
# Disable in production
telescope:
  enabled: false
```

#### `telescope.base-package`

- **Type:** `String`
- **Default:** `""` (empty — captures everything)
- **Description:** The base package of your application. Used by the **Log Watcher** and **Event Watcher** to filter entries:
  - **Log Watcher:** Only captures logs from loggers within this package
  - **Event Watcher:** Only captures `ApplicationEvent` instances from classes within this package

```yaml
telescope:
  base-package: com.mycompany.myapp
```

When empty:
- The Log Watcher captures all logs at INFO level and above (except Telescope's own)
- The Event Watcher captures all events (except Spring framework internals and Telescope's own)

#### `telescope.max-entries`

- **Type:** `int`
- **Default:** `1000`
- **Description:** Maximum number of entries kept in memory **per entry type**. Since there are 9 types, the theoretical maximum is `9 x max-entries` entries. When the limit is reached, the oldest entries are evicted (FIFO).

```yaml
telescope:
  max-entries: 500   # Lower for constrained environments
  max-entries: 5000  # Higher for detailed debugging sessions
```

#### `telescope.prune-hours`

- **Type:** `int`
- **Default:** `24`
- **Description:** Entries older than this many hours are automatically removed by the pruner.

#### `telescope.prune-interval-ms`

- **Type:** `long`
- **Default:** `3600000` (1 hour)
- **Description:** How often the pruner runs, in milliseconds. Uses Spring's `@Scheduled(fixedRate)`.

```yaml
telescope:
  prune-interval-ms: 1800000  # Every 30 minutes
```

### Dashboard Properties

#### `telescope.base-path`

- **Type:** `String`
- **Default:** `/telescope`
- **Description:** The URL path where the dashboard and API are served. The dashboard HTML is at `{base-path}/index.html` and the API is at `{base-path}/api/*`.

```yaml
telescope:
  base-path: /admin/debug/telescope
```

This would make the dashboard available at `http://localhost:8080/admin/debug/telescope`.

#### `telescope.ignored-prefixes`

- **Type:** `Set<String>`
- **Default:** `["/actuator", "/swagger", "/v3/api-docs"]`
- **Description:** URL path prefixes that the Request Watcher should ignore. The `telescope.base-path` is always added automatically to prevent Telescope from recording its own API calls.

```yaml
telescope:
  ignored-prefixes:
    - /actuator
    - /swagger
    - /v3/api-docs
    - /health
    - /static
    - /favicon.ico
```

### Multi-Tenancy Properties

#### `telescope.tenant-pattern`

- **Type:** `String`
- **Default:** `""` (disabled)
- **Description:** A regex pattern applied to the request URI to extract a tenant/organization ID. The **first capture group** is used as the tenant ID. This is used by the `DefaultTelescopeUserProvider` — if you provide a custom `TelescopeUserProvider`, this property is ignored (since you control tenant resolution yourself).

```yaml
# Extract org ID from URLs like /api/organizations/42/users
telescope:
  tenant-pattern: "/organizations/(\\d+)"

# Extract tenant from subdomain-style paths like /tenant/acme/...
telescope:
  tenant-pattern: "/tenant/([^/]+)"
```

### Watcher Toggle Properties

Each watcher can be individually enabled or disabled. All default to `true`.

| Property | Default | Watcher |
|----------|---------|---------|
| `telescope.watchers.requests` | `true` | HTTP Request/Response |
| `telescope.watchers.queries` | `true` | SQL Queries (Hibernate) |
| `telescope.watchers.exceptions` | `true` | Exception Recorder |
| `telescope.watchers.logs` | `true` | Log Appender (Logback) |
| `telescope.watchers.schedules` | `true` | @Scheduled Tasks |
| `telescope.watchers.cache` | `true` | @Cacheable Operations |
| `telescope.watchers.events` | `true` | Spring Events |
| `telescope.watchers.mail` | `true` | Mail Sending |
| `telescope.watchers.models` | `true` | Hibernate Entity Changes |

**Note:** Even if a watcher is enabled, it will **not activate** if its required classpath dependencies are missing. For example, `telescope.watchers.queries=true` has no effect if Hibernate is not on the classpath.

```yaml
# Only capture requests and exceptions
telescope:
  watchers:
    requests: true
    exceptions: true
    queries: false
    logs: false
    schedules: false
    cache: false
    events: false
    mail: false
    models: false
```

## Environment-Specific Configuration

### Development

```yaml
# application-dev.yml
telescope:
  enabled: true
  base-package: com.mycompany.myapp
  max-entries: 2000
```

### Staging

```yaml
# application-staging.yml
telescope:
  enabled: true
  base-package: com.mycompany.myapp
  max-entries: 500
  watchers:
    mail: false  # Don't capture mail in staging
```

### Production

```yaml
# application-prod.yml
telescope:
  enabled: false  # Completely disabled
```

## Using Properties Files

All YAML properties can be expressed in `application.properties` format:

```properties
telescope.enabled=true
telescope.base-package=com.mycompany.myapp
telescope.max-entries=1000
telescope.prune-hours=24
telescope.prune-interval-ms=3600000
telescope.base-path=/telescope
telescope.tenant-pattern=/organizations/(\\d+)
telescope.watchers.requests=true
telescope.watchers.queries=true
telescope.watchers.exceptions=true
telescope.watchers.logs=true
telescope.watchers.schedules=true
telescope.watchers.cache=true
telescope.watchers.events=true
telescope.watchers.mail=true
telescope.watchers.models=true
```
