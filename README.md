<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen" alt="Spring Boot 3.x">
  <img src="https://img.shields.io/badge/Java-17%2B-blue" alt="Java 17+">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License MIT">
  <img src="https://img.shields.io/badge/version-0.1.0-purple" alt="Version 0.1.0">
</p>

# Spring Telescope

A **zero-configuration**, real-time debugging and observability dashboard for Spring Boot 3 applications. Inspired by [Laravel Telescope](https://laravel.com/docs/telescope), it captures HTTP requests, SQL queries, exceptions, logs, cache operations, scheduled tasks, application events, mail, and Hibernate model changes — with **in-memory** or **database** storage.

Add one Maven dependency, start your app, and open `/telescope`.

---

## Table of Contents

- [Quick Start](#quick-start)
- [Dashboard Preview](#dashboard-preview)
- [Features](#features)
- [Installation](#installation)
- [Configuration](#configuration)
- [Storage](#storage)
  - [In-Memory (default)](#in-memory-default)
  - [Database (JPA)](#database-jpa)
  - [Custom Storage](#custom-storage)
- [Watchers](#watchers)
- [Customization](#customization)
  - [Custom User Provider](#custom-user-provider)
  - [Custom Filter Provider](#custom-filter-provider)
- [REST API](#rest-api)
- [Architecture](#architecture)
- [Multi-Tenancy Support](#multi-tenancy-support)
- [Security](#security)
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

### Duplicate Grouping

All views support **automatic duplicate grouping**. Entries with identical signatures (e.g., the same SQL query executed multiple times, or the same endpoint called repeatedly) are collapsed into a single row with an **"xN" badge** showing the count. Click the badge to expand and see each individual entry.

Grouping criteria by type:

| Type | Grouped by |
|------|------------|
| QUERY | SQL statement |
| REQUEST | method + URI + status |
| EXCEPTION | class + message |
| LOG | level + logger + message |
| SCHEDULE | class + method |
| CACHE | operation + cache name + key |
| EVENT | event + event class |
| MAIL | subject + recipient |
| MODEL | action + entity class + entity ID |

Toggle between **Grouped** and **Flat** mode using the button in the toolbar.

All views also support **pagination**, **full-text search**, **user filtering**, **tenant filtering**, and **related entry grouping** (batch IDs link all entries from the same HTTP request).

---

## Features

| Feature | Description |
|---------|-------------|
| **Zero configuration** | Works out of the box with sensible defaults |
| **Auto-detection** | Watchers only activate when their dependencies are on the classpath |
| **Dual storage** | In-memory (default) or database (JPA) — switch with one property |
| **Auto-pruning** | Old entries are automatically cleaned up (configurable) |
| **Duplicate grouping** | Identical entries are collapsed with expandable count badges |
| **Multi-tenancy** | Built-in support for tenant/organization context via URL pattern or custom provider |
| **Extensible** | Replace user provider, filter provider, or storage with your own implementations |
| **Batch correlation** | All entries from the same HTTP request are linked by a batch ID |
| **Access token** | Protect the dashboard with a configurable token (query param or header) |
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
  base-package: com.mycompany.myapp

  # Maximum entries kept in memory PER TYPE (only for in-memory storage)
  max-entries: 1000

  # Automatically prune entries older than N hours
  prune-hours: 24

  # How often the pruner runs (ms)
  prune-interval-ms: 3600000

  # URL path where the dashboard is served
  base-path: /telescope

  # URL prefixes to exclude from request recording
  ignored-prefixes:
    - /actuator
    - /swagger
    - /v3/api-docs

  # Regex to extract tenant ID from the request URL (first capture group)
  tenant-pattern: "/organizations/(\\d+)"

  # Access token to protect the dashboard (leave empty to disable)
  access-token: ""

  # Storage backend: "memory" (default) or "database"
  storage: memory

  # How often buffered entries are flushed to the database (ms, only for database storage)
  flush-interval-ms: 2000

  # Enable/disable individual watchers
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

### Properties reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `telescope.enabled` | `boolean` | `true` | Master switch for the entire library |
| `telescope.base-package` | `String` | `""` | Base package for log/event filtering |
| `telescope.max-entries` | `int` | `1000` | Max entries per type (in-memory only) |
| `telescope.prune-hours` | `int` | `24` | Auto-prune entries older than N hours |
| `telescope.prune-interval-ms` | `long` | `3600000` | Pruner execution interval (ms) |
| `telescope.base-path` | `String` | `/telescope` | Dashboard URL path |
| `telescope.ignored-prefixes` | `Set<String>` | `/actuator, /swagger, /v3/api-docs` | URL prefixes to ignore |
| `telescope.tenant-pattern` | `String` | `""` | Regex for tenant ID extraction from URL |
| `telescope.access-token` | `String` | `""` | Token to protect the dashboard |
| `telescope.storage` | `String` | `memory` | Storage backend: `memory` or `database` |
| `telescope.flush-interval-ms` | `long` | `2000` | DB flush interval (database storage only) |
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

## Storage

### In-Memory (default)

The default storage keeps entries in concurrent data structures in memory. No database or additional configuration is required. Entries are lost on application restart.

```yaml
telescope:
  storage: memory       # default
  max-entries: 1000     # per type (9 types x 1000 = up to 9000 entries)
```

Best for: development, debugging, lightweight production monitoring where persistence is not needed.

### Database (JPA)

For persistent storage, Telescope can write entries to your application's database using JPA. Entries survive application restarts and can be queried across sessions.

**Prerequisites:** `spring-boot-starter-data-jpa` must be on the classpath with a configured datasource.

**1. Enable database storage:**

```yaml
telescope:
  storage: database
```

**2. Configure your datasource** (if not already done):

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: mypassword
  jpa:
    hibernate:
      ddl-auto: update    # creates the telescope_entries table automatically
```

That's it. Telescope will automatically:

- Create a `telescope_entries` table with indexed columns (`type`, `createdAt`, `batchId`, `userIdentifier`, `tenantId`)
- Buffer entries in memory and flush them to the database every 2 seconds (configurable via `telescope.flush-interval-ms`)
- Query the database for all dashboard operations (pagination, filtering, search)

**Table schema (`telescope_entries`):**

| Column | Type | Description |
|--------|------|-------------|
| `uuid` | `VARCHAR(36)` PK | Entry unique identifier |
| `type` | `VARCHAR(20)` | Entry type (REQUEST, QUERY, etc.) |
| `created_at` | `TIMESTAMP` | When the entry was recorded |
| `batch_id` | `VARCHAR(36)` | Batch correlation ID |
| `content_json` | `TEXT` | Entry content as JSON |
| `user_identifier` | `VARCHAR(255)` | User who triggered the entry |
| `tenant_id` | `VARCHAR(255)` | Tenant/organization ID |
| `tags_json` | `VARCHAR(2000)` | Tags as JSON array |

**Supported databases:** Any database supported by Hibernate — PostgreSQL, MySQL, MariaDB, H2, Oracle, SQL Server, etc.

**Tuning the flush interval:**

```yaml
telescope:
  storage: database
  flush-interval-ms: 5000   # flush every 5 seconds (reduces DB writes)
```

A shorter interval means entries appear in the dashboard faster. A longer interval reduces database write pressure. The flusher also runs on application shutdown (`@PreDestroy`) to avoid losing buffered entries.

### Custom Storage

You can replace either built-in storage by providing your own `TelescopeStorage` bean:

```java
@Bean
public TelescopeStorage telescopeStorage() {
    return new MyRedisTelescopeStorage();
}
```

The `@ConditionalOnMissingBean` on the default storage beans ensures your implementation takes precedence automatically.

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

A built-in `@ControllerAdvice` (`TelescopeExceptionHandler`) captures unhandled exceptions automatically. It runs with lowest precedence so your application-defined handlers take priority.

You can also inject `TelescopeExceptionRecorder` to record exceptions manually:

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
- Duration (used for hit/miss heuristic: < 500 microseconds = HIT)

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

Intercepts `MailSender.send()` and `JavaMailSender.send()` calls and captures:

- To, From, Subject
- Body preview (truncated at 500 characters)
- Message type (simple / mime)
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

By default, Telescope uses Spring Security's `SecurityContextHolder` (via reflection) to identify the current user. If Spring Security is not on the classpath, user identification is disabled.

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
        MyUser user = authService.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    @Override
    public String getCurrentTenantId() {
        MyUser user = authService.getCurrentUser();
        return user != null ? String.valueOf(user.getOrganizationId()) : null;
    }
}
```

The `@ConditionalOnMissingBean` on the default provider ensures your implementation takes precedence automatically.

### Custom Filter Provider

The dashboard has dropdown filters for users and tenants. By default, these are populated from distinct values found in the storage. To show richer data (e.g., full names from your database), implement `TelescopeFilterProvider`:

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
├── TelescopeAutoConfiguration.java       # Spring Boot auto-configuration entry point
├── TelescopeProperties.java              # @ConfigurationProperties for all settings
├── TelescopeApiResponse.java             # Standard API response wrapper
├── context/
│   ├── TelescopeUserProvider.java        # Interface: who is the current user?
│   ├── DefaultTelescopeUserProvider.java # Default: uses Spring Security via reflection
│   └── TelescopeBatchContext.java        # ThreadLocal batch ID for request correlation
├── controller/
│   └── TelescopeController.java          # REST API endpoints
├── filter/
│   ├── TelescopeFilterProvider.java      # Interface: dropdown filter data
│   └── DefaultTelescopeFilterProvider.java # Default: extracts from storage
├── model/
│   ├── TelescopeEntry.java               # Entry data model
│   └── TelescopeEntryType.java           # Enum: REQUEST, QUERY, LOG, etc.
├── storage/
│   ├── TelescopeStorage.java             # Interface: storage abstraction
│   ├── InMemoryTelescopeStorage.java     # Default: concurrent in-memory storage
│   └── jpa/
│       ├── TelescopeJpaAutoConfiguration.java # Auto-config for database storage
│       ├── JpaTelescopeStorage.java      # JPA storage implementation with buffering
│       ├── TelescopeEntryEntity.java     # JPA entity (telescope_entries table)
│       ├── TelescopeEntryRepository.java # Spring Data JPA repository
│       └── TelescopeStorageFlusher.java  # Periodic buffer → database flusher
└── watcher/
    ├── TelescopeRequestFilter.java       # HTTP request/response capture
    ├── TelescopeContextCaptureFilter.java # Captures user context from SecurityContext
    ├── TelescopeSecurityFilter.java      # Access token protection
    ├── TelescopeQueryInspector.java      # Hibernate SQL statement inspector
    ├── TelescopeExceptionHandler.java    # @ControllerAdvice for unhandled exceptions
    ├── TelescopeExceptionRecorder.java   # Exception recording service
    ├── TelescopeLogAppender.java         # Logback appender
    ├── TelescopeScheduleAspect.java      # @Scheduled AOP aspect
    ├── TelescopeCacheAspect.java         # @Cacheable/@CacheEvict AOP aspect
    ├── TelescopeEventWatcher.java        # Spring ApplicationEvent listener
    ├── TelescopeMailWatcher.java         # MailSender AOP aspect
    ├── TelescopeModelListener.java       # Hibernate entity change listener
    ├── TelescopeHibernateIntegrator.java # Hibernate SPI integrator
    └── TelescopePruner.java              # Scheduled entry cleanup
```

### How auto-configuration works

1. Spring Boot discovers `TelescopeJpaAutoConfiguration` and `TelescopeAutoConfiguration` via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
2. If `telescope.storage=database` and JPA is on the classpath, `TelescopeJpaAutoConfiguration` creates the JPA storage bean **before** the main auto-configuration (via `@AutoConfigureBefore`)
3. `TelescopeAutoConfiguration` checks `@ConditionalOnProperty(telescope.enabled)` and creates core beans with `@ConditionalOnMissingBean` — your custom beans (or the JPA beans) always take precedence
4. Each watcher bean has its own conditions:
   - `@ConditionalOnProperty` for the watcher toggle
   - `@ConditionalOnClass` for classpath dependencies (AOP, Hibernate, Mail, Logback)
5. The Hibernate integrator is registered via `META-INF/services/org.hibernate.integrator.spi.Integrator`

### Entry lifecycle

```
HTTP Request arrives
  → TelescopeSecurityFilter checks access token (if configured)
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

Storage:
  → In-memory: entries stored directly in concurrent data structures
  → Database: entries buffered in queue → flushed to DB every N ms
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

## Security

**Telescope is a development/debugging tool.** In production, you should secure the dashboard.

### Access token (built-in)

The simplest way to protect the dashboard — no Spring Security required:

```yaml
telescope:
  access-token: my-secret-token
```

Access the dashboard via:
- Query parameter: `http://localhost:8080/telescope?token=my-secret-token`
- Header: `X-Telescope-Token: my-secret-token`

Requests without a valid token receive a `403 Forbidden` response.

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

### Sensitive data handling

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

### Database storage: entries not appearing

1. Verify `telescope.storage=database` is set
2. Check your datasource configuration and that the DB is reachable
3. Entries are buffered and flushed every `telescope.flush-interval-ms` (default 2s) — wait a moment
4. Check logs for JPA/Hibernate errors
5. Ensure `spring.jpa.hibernate.ddl-auto=update` (or create the table manually)

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
