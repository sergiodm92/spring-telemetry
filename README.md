<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x">
  <img src="https://img.shields.io/badge/Java-17%2B-blue" alt="Java 17+">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License MIT">
  <img src="https://img.shields.io/badge/version-0.1.0-purple" alt="Version 0.1.0">
</p>

# Spring Telescope

A **zero-configuration**, real-time debugging and observability dashboard for Spring Boot 3 applications. Inspired by [Laravel Telescope](https://laravel.com/docs/telescope), it captures HTTP requests, SQL queries, exceptions, logs, cache operations, scheduled tasks, application events, mail, and Hibernate model changes — all stored **in-memory** with no database required.

Add one Maven dependency, start your app, and open `/telescope`.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Dashboard Preview](#dashboard-preview)
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Watchers](#watchers)
- [Customization](#customization)
  - [Custom User Provider](#custom-user-provider)
  - [Custom Filter Provider](#custom-filter-provider)
  - [Custom Storage](#custom-storage)
- [REST API](#rest-api)
- [Architecture](#architecture)
- [Multi-Tenancy Support](#multi-tenancy-support)
- [Security Considerations](#security-considerations)
- [Troubleshooting](#troubleshooting)
- [Requirements](#requirements)
- [License](#license)

---

## Quick Start

**1. Add the dependency:**

```xml
<dependency>
    <groupId>dev.springtelescope</groupId>
    <artifactId>spring-telescope</artifactId>
    <version>0.1.0</version>
</dependency>
```

**2. (Optional) Set your base package** to filter logs and events:

```yaml
telescope:
  base-package: com.mycompany.myapp
```

**3. Start your application and open:**

```
http://localhost:8080/telescope
```

That's it. All 9 watchers are enabled by default and will auto-detect which dependencies are available on your classpath.

---

## Dashboard Preview

The dashboard is a single-page application built with Alpine.js and Tailwind CSS. It provides:

- **Dashboard** — overview with entry counts per type and recent requests
- **Requests** — full HTTP request/response inspector with headers, body, duration, and status
- **Exceptions** — exception class, message, stack trace, and request context
- **Queries** — all SQL statements intercepted by Hibernate (SELECT, INSERT, UPDATE, DELETE)
- **Logs** — application log entries (INFO, WARN, ERROR) with MDC context
- **Schedule** — `@Scheduled` task executions with duration and failure tracking
- **Cache** — `@Cacheable`, `@CachePut`, `@CacheEvict` operations with hit/miss detection
- **Events** — Spring `ApplicationEvent` from your base package with payload extraction
- **Mail** — outgoing `SimpleMailMessage` details (to, from, subject, body preview)
- **Models** — Hibernate entity inserts, updates (with changed fields), and deletes

All views support **pagination**, **full-text search**, **user filtering**, **tenant filtering**, and **related entry grouping** (batch IDs link all entries from the same HTTP request).

---

## Features

| Feature | Description |
|---------|-------------|
| **Zero configuration** | Works out of the box with sensible defaults |
| **Auto-detection** | Watchers only activate when their dependencies are on the classpath |
| **In-memory storage** | No database required — entries stored in concurrent data structures |
| **Auto-pruning** | Old entries are automatically cleaned up (configurable) |
| **Multi-tenancy** | Built-in support for tenant/organization context via URL pattern or custom provider |
| **Extensible** | Replace user provider, filter provider, or storage with your own implementations |
| **Batch correlation** | All entries from the same HTTP request are linked by a batch ID |
| **Security-aware** | Masks `Authorization` and `Cookie` headers, body truncation |
| **Non-intrusive** | All watchers catch and swallow their own exceptions — Telescope never breaks your app |
| **Toggle at runtime** | Enable/disable recording via the dashboard or API without restart |

---

## Installation

### Maven

```xml
<dependency>
    <groupId>dev.springtelescope</groupId>
    <artifactId>spring-telescope</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'dev.springtelescope:spring-telescope:0.1.0'
```

### Building from source

```bash
git clone https://github.com/sergiodelr/spring-telescope.git
cd spring-telescope
mvn clean install
```

---

## Configuration

All configuration is optional. Add properties to your `application.yml` or `application.properties`:

```yaml
telescope:
  # Master switch — set to false to disable Telescope entirely
  enabled: true

  # Your application's base package — used to filter logs and events
  # If empty, the log watcher captures ALL application logs (INFO+)
  # If set, only logs from this package (and sub-packages) are captured
  base-package: com.mycompany.myapp

  # Maximum number of entries kept in memory PER TYPE
  # (9 types x 1000 = up to 9000 entries total)
  max-entries: 1000

  # Automatically prune entries older than N hours
  prune-hours: 24

  # How often the pruner runs (in milliseconds)
  prune-interval-ms: 3600000

  # URL path where the dashboard is served
  base-path: /telescope

  # URL prefixes to exclude from request recording
  # The telescope base-path is always excluded automatically
  ignored-prefixes:
    - /actuator
    - /swagger
    - /v3/api-docs

  # Regex pattern to extract tenant/organization ID from the request URL
  # The first capture group becomes the tenant ID
  # Leave empty to disable URL-based tenant extraction
  tenant-pattern: "/organizations/(\\d+)"

  # Enable/disable individual watchers
  watchers:
    requests: true      # HTTP request/response recording
    queries: true       # SQL statement interception (requires Hibernate)
    exceptions: true    # Exception recording
    logs: true          # Log appender (requires Logback)
    schedules: true     # @Scheduled task tracking (requires AOP)
    cache: true         # Cache operation tracking (requires AOP)
    events: true        # Spring ApplicationEvent capturing
    mail: true          # Mail sending tracking (requires AOP + spring-mail)
    models: true        # Hibernate entity change tracking (requires Hibernate)
```

### Properties reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `telescope.enabled` | `boolean` | `true` | Master switch for the entire library |
| `telescope.base-package` | `String` | `""` | Base package for log/event filtering |
| `telescope.max-entries` | `int` | `1000` | Max entries per type in memory |
| `telescope.prune-hours` | `int` | `24` | Auto-prune entries older than N hours |
| `telescope.prune-interval-ms` | `long` | `3600000` | Pruner execution interval (ms) |
| `telescope.base-path` | `String` | `/telescope` | Dashboard URL path |
| `telescope.ignored-prefixes` | `Set<String>` | `/actuator, /swagger, /v3/api-docs` | URL prefixes to ignore |
| `telescope.tenant-pattern` | `String` | `""` | Regex for tenant ID extraction from URL |
| `telescope.watchers.requests` | `boolean` | `true` | Enable request watcher |
| `telescope.watchers.queries` | `boolean` | `true` | Enable query watcher |
| `telescope.watchers.exceptions` | `boolean` | `true` | Enable exception watcher |
| `telescope.watchers.logs` | `boolean` | `true` | Enable log watcher |
| `telescope.watchers.schedules` | `boolean` | `true` | Enable schedule watcher |
| `telescope.watchers.cache` | `boolean` | `true` | Enable cache watcher |
| `telescope.watchers.events` | `boolean` | `true` | Enable event watcher |
| `telescope.watchers.mail` | `boolean` | `true` | Enable mail watcher |
| `telescope.watchers.models` | `boolean` | `true` | Enable model watcher |

---

## Watchers

Each watcher monitors a specific aspect of your application. They are conditionally activated based on classpath dependencies and configuration.

### Request Watcher

**Requires:** `spring-boot-starter-web` (always present)

Captures every HTTP request and response passing through your application:

- HTTP method, URI, query string
- Request/response headers (sensitive headers like `Authorization` and `Cookie` are masked)
- Request/response body (truncated at 10KB)
- Status code, duration, IP address, content types
- User and tenant context (captured via `TelescopeContextCaptureFilter`)

The request watcher runs at `Ordered.HIGHEST_PRECEDENCE + 10` to wrap the entire filter chain. A companion `TelescopeContextCaptureFilter` (at `Order(2)`) captures user identity from Spring Security while the `SecurityContext` is still available.

**Ignored paths:** The `telescope.ignored-prefixes` set plus the `telescope.base-path` are automatically excluded.

### Query Watcher

**Requires:** `spring-boot-starter-data-jpa` (Hibernate)

Intercepts all SQL statements executed by Hibernate using the `StatementInspector` SPI. Captures:

- The full SQL statement
- SQL type classification (SELECT, INSERT, UPDATE, DELETE, DDL, OTHER)
- Batch ID correlation with the originating request

Health-check queries (`SELECT 1`, `SELECT VERSION`) are automatically excluded.

### Exception Watcher

**Requires:** Core (no additional dependencies)

Provides a `TelescopeExceptionRecorder` bean that you can inject into your `@ControllerAdvice` or exception handlers:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private final TelescopeExceptionRecorder recorder;

    public GlobalExceptionHandler(TelescopeExceptionRecorder recorder) {
        this.recorder = recorder;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception ex) {
        recorder.record(ex);
        return ResponseEntity.status(500).body("Internal Server Error");
    }
}
```

Captures: exception class, message, stack trace (truncated at 5KB), file/line, method location, request URI, cause chain.

### Log Watcher

**Requires:** `ch.qos.logback:logback-classic`

Attaches a custom Logback `AppenderBase` to the root logger. Captures:

- Log level (INFO, WARN, ERROR — DEBUG and TRACE are excluded)
- Formatted message
- Logger name
- Thread name
- Exception (if attached to the log event)
- MDC context map

**Filtering:** If `telescope.base-package` is set, only logs from that package (and sub-packages) are captured. Telescope's own logs (`dev.springtelescope.*`) are always excluded to prevent infinite loops.

### Schedule Watcher

**Requires:** `spring-boot-starter-aop` (AspectJ)

Wraps every `@Scheduled` method with an aspect that captures:

- Target class and method name
- Execution duration
- Status (completed/failed)
- Exception details on failure
- Batch ID (each scheduled execution gets its own batch)

### Cache Watcher

**Requires:** `spring-boot-starter-aop` (AspectJ)

Intercepts `@Cacheable`, `@CacheEvict`, and `@CachePut` annotations:

- Cache operation type (HIT, MISS, EVICT, PUT)
- Cache name
- Cache key (derived from method signature and arguments)
- Duration (used for hit/miss heuristic: < 2ms = HIT)

### Event Watcher

**Requires:** Core (no additional dependencies)

Listens for all Spring `ApplicationEvent` instances. Captures:

- Event class name
- Source object type
- Payload (extracted via reflection from getter methods)
- Tags

**Filtering:** If `telescope.base-package` is set, only events from that package are captured. Spring framework internal events (`org.springframework.*`) and Telescope's own events are always excluded.

### Mail Watcher

**Requires:** `spring-boot-starter-aop` + `spring-boot-starter-mail`

Intercepts `MailSender.send()` calls and captures:

- To, From, Subject
- Body preview (truncated at 500 characters)
- Tags

### Model Watcher

**Requires:** `spring-boot-starter-data-jpa` (Hibernate)

Registers as a Hibernate `Integrator` via the `META-INF/services` SPI. Listens for:

- `PostInsertEvent` — entity created
- `PostUpdateEvent` — entity updated (with list of changed field names)
- `PostDeleteEvent` — entity deleted

Captures: entity class, entity ID, action, changed fields.

---

## Customization

### Custom User Provider

By default, Telescope uses Spring Security's `SecurityContextHolder` to identify the current user. If Spring Security is not on the classpath, user identification is disabled.

To customize how users are identified, implement `TelescopeUserProvider` and register it as a Spring bean:

```java
@Component
public class MyTelescopeUserProvider implements TelescopeUserProvider {

    private final MyAuthService authService;

    public MyTelescopeUserProvider(MyAuthService authService) {
        this.authService = authService;
    }

    @Override
    public String getCurrentUserIdentifier() {
        // Return email, username, user ID, or any string identifier
        MyUser user = authService.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    @Override
    public String getCurrentTenantId() {
        // Return tenant/organization ID if applicable
        MyUser user = authService.getCurrentUser();
        return user != null ? String.valueOf(user.getOrganizationId()) : null;
    }
}
```

The `@ConditionalOnMissingBean` on the default provider ensures your implementation takes precedence automatically.

### Custom Filter Provider

The dashboard has dropdown filters for users and tenants. By default, these are populated from distinct values found in the in-memory storage. To show richer data (e.g., full names from your database), implement `TelescopeFilterProvider`:

```java
@Component
public class MyTelescopeFilterProvider implements TelescopeFilterProvider {

    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;

    public MyTelescopeFilterProvider(UserRepository userRepo, OrganizationRepository orgRepo) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
    }

    @Override
    public List<Map<String, String>> getUsers() {
        return userRepo.findAll().stream()
            .map(u -> Map.of("id", u.getEmail(), "name", u.getFullName()))
            .sorted(Comparator.comparing(m -> m.get("name")))
            .toList();
    }

    @Override
    public List<Map<String, String>> getTenants() {
        return orgRepo.findAll().stream()
            .map(o -> Map.of("id", String.valueOf(o.getId()), "name", o.getName()))
            .sorted(Comparator.comparing(m -> m.get("name")))
            .toList();
    }
}
```

Each map **must** contain `"id"` and `"name"` keys. The `"id"` is used for filtering, and `"name"` is displayed in the dropdown.

### Custom Storage

You can replace the default in-memory storage by providing your own `TelescopeStorage` bean:

```java
@Bean
public TelescopeStorage telescopeStorage() {
    return new MyCustomTelescopeStorage();
}
```

---

## REST API

Telescope exposes a REST API under `{base-path}/api`. All responses use this format:

```json
{
  "success": true,
  "message": "Description",
  "data": { ... }
}
```

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/telescope/api/entries?type={TYPE}&page=0&size=50` | List entries by type with pagination |
| `GET` | `/telescope/api/entries/{uuid}` | Get a single entry by UUID |
| `GET` | `/telescope/api/entries/{uuid}/related` | Get all entries in the same batch |
| `GET` | `/telescope/api/filters` | Get filter dropdown options |
| `GET` | `/telescope/api/stats` | Get entry counts per type |
| `GET` | `/telescope/api/status` | Get enabled status + stats |
| `GET` | `/telescope/api/tags` | Get all distinct tags |
| `POST` | `/telescope/api/toggle` | Toggle recording on/off |
| `POST` | `/telescope/api/prune?hours=24` | Prune entries older than N hours |
| `DELETE` | `/telescope/api/entries?type={TYPE}` | Clear entries (optionally by type) |

### Entry types

`REQUEST`, `EXCEPTION`, `QUERY`, `LOG`, `SCHEDULE`, `CACHE`, `EVENT`, `MAIL`, `MODEL`

### Query parameters for listing entries

| Parameter | Required | Description |
|-----------|----------|-------------|
| `type` | Yes | Entry type (see above) |
| `page` | No (default: 0) | Page number |
| `size` | No (default: 50) | Page size |
| `search` | No | Full-text search across entry content |
| `userIdentifier` | No | Filter by user identifier |
| `tenantId` | No | Filter by tenant ID |
| `method` | No | Filter by HTTP method (requests only) |
| `statusGroup` | No | Filter by status group: `2xx`, `3xx`, `4xx`, `5xx` (requests only) |

### Example

```bash
# Get the latest 10 requests
curl http://localhost:8080/telescope/api/entries?type=REQUEST&size=10

# Get stats
curl http://localhost:8080/telescope/api/stats

# Toggle recording
curl -X POST http://localhost:8080/telescope/api/toggle

# Prune old entries
curl -X POST http://localhost:8080/telescope/api/prune?hours=12
```

---

## Architecture

```
spring-telescope/
├── TelescopeAutoConfiguration.java    # Spring Boot auto-configuration entry point
├── TelescopeProperties.java           # @ConfigurationProperties for all settings
├── TelescopeApiResponse.java          # Standard API response wrapper
├── context/
│   ├── TelescopeUserProvider.java     # Interface: who is the current user?
│   ├── DefaultTelescopeUserProvider   # Default: uses Spring Security
│   └── TelescopeBatchContext.java     # ThreadLocal batch ID for request correlation
├── controller/
│   └── TelescopeController.java       # REST API endpoints
├── filter/
│   ├── TelescopeFilterProvider.java   # Interface: dropdown filter data
│   └── DefaultTelescopeFilterProvider # Default: extracts from storage
├── model/
│   ├── TelescopeEntry.java            # Entry data model
│   └── TelescopeEntryType.java        # Enum: REQUEST, QUERY, LOG, etc.
├── storage/
│   └── TelescopeStorage.java          # In-memory concurrent storage
└── watcher/
    ├── TelescopeRequestFilter.java        # HTTP request/response capture
    ├── TelescopeContextCaptureFilter.java # Captures user context from SecurityContext
    ├── TelescopeQueryInspector.java       # Hibernate SQL statement inspector
    ├── TelescopeExceptionRecorder.java    # Exception recording service
    ├── TelescopeLogAppender.java          # Logback appender
    ├── TelescopeScheduleAspect.java       # @Scheduled AOP aspect
    ├── TelescopeCacheAspect.java          # @Cacheable/@CacheEvict AOP aspect
    ├── TelescopeEventWatcher.java         # Spring ApplicationEvent listener
    ├── TelescopeMailWatcher.java          # MailSender AOP aspect
    ├── TelescopeModelListener.java        # Hibernate entity change listener
    ├── TelescopeHibernateIntegrator.java   # Hibernate SPI integrator
    └── TelescopePruner.java               # Scheduled entry cleanup
```

### How auto-configuration works

1. Spring Boot discovers `TelescopeAutoConfiguration` via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
2. The `@ConditionalOnProperty` check ensures the whole library is disabled if `telescope.enabled=false`
3. Core beans (`TelescopeStorage`, `TelescopeUserProvider`, `TelescopeFilterProvider`) are created with `@ConditionalOnMissingBean` — your custom beans always take precedence
4. Each watcher bean has its own conditions:
   - `@ConditionalOnProperty` for the watcher toggle
   - `@ConditionalOnClass` for classpath dependencies (AOP, Hibernate, Mail, Logback)
5. The Hibernate integrator is registered via `META-INF/services/org.hibernate.integrator.spi.Integrator`

### Entry lifecycle

```
HTTP Request arrives
  → TelescopeRequestFilter creates batch ID (ThreadLocal)
  → TelescopeContextCaptureFilter captures user from SecurityContext
  → Request proceeds through your app
    → Queries intercepted by TelescopeQueryInspector
    → Logs captured by TelescopeLogAppender
    → Cache operations intercepted by TelescopeCacheAspect
    → Events captured by TelescopeEventWatcher
    → Model changes captured by TelescopeModelListener
    → Exceptions recorded by TelescopeExceptionRecorder
  → Response completes
  → TelescopeRequestFilter records the full request/response
  → All entries share the same batch ID → viewable as "Related Entries"
```

---

## Multi-Tenancy Support

Telescope supports multi-tenant applications in two ways:

### 1. URL-based tenant extraction

Set a regex pattern that extracts the tenant ID from the request URL:

```yaml
telescope:
  tenant-pattern: "/organizations/(\\d+)"
```

This pattern will extract `42` from URLs like `/api/organizations/42/users`. The first capture group is used as the tenant ID.

### 2. Custom user provider

For more complex tenant resolution, implement `TelescopeUserProvider`:

```java
@Component
public class MyUserProvider implements TelescopeUserProvider {
    @Override
    public String getCurrentUserIdentifier() { return "user@example.com"; }

    @Override
    public String getCurrentTenantId() {
        // Resolve from request header, JWT claim, database, etc.
        return TenantContext.getCurrentTenantId();
    }
}
```

The tenant ID is stored on every entry and can be used to filter the dashboard.

---

## Security Considerations

**Telescope is a development/debugging tool.** In production, you should secure the dashboard:

### Restrict access with Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/telescope/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### Disable in production

```yaml
# application-prod.yml
telescope:
  enabled: false
```

### Sensitive data

- `Authorization` and `Cookie` headers are automatically masked with `***`
- Request/response bodies are truncated at 10KB
- Stack traces are truncated at 5KB
- Mail body previews are truncated at 500 characters

---

## Troubleshooting

### Dashboard shows "No requests recorded"

1. Verify Telescope is enabled: `GET /telescope/api/status`
2. Check that your request path is not in the `ignored-prefixes` list
3. Ensure you are not using `telescope.enabled=false`

### Logs are not captured

1. Ensure `logback-classic` is on the classpath (included by `spring-boot-starter-web`)
2. Set `telescope.base-package` to your application's package (e.g., `com.myapp`)
3. Only `INFO`, `WARN`, and `ERROR` levels are captured

### Queries are not captured

1. Ensure `spring-boot-starter-data-jpa` is on the classpath
2. The query inspector uses Hibernate's `StatementInspector` — if another inspector is already configured, they may conflict

### Cache operations are not captured

1. Ensure `spring-boot-starter-aop` is on the classpath
2. Cache annotations must be on Spring-managed beans (not on private methods or self-invocations)

### Events are not captured

1. Set `telescope.base-package` to your application's package
2. Only events from classes within that package are captured
3. Spring internal events are automatically excluded

### The dashboard path conflicts with my app

Change it:

```yaml
telescope:
  base-path: /admin/telescope
```

---

## Requirements

- **Java:** 17 or higher
- **Spring Boot:** 3.x (tested with 3.2.x)
- **Required:** `spring-boot-starter-web`
- **Optional:** `spring-boot-starter-aop`, `spring-boot-starter-data-jpa`, `spring-boot-starter-mail`, `spring-boot-starter-security`, `logback-classic`

---

## License

MIT License. See [LICENSE](LICENSE) for details.
