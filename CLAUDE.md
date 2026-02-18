# Spring Telescope - Project Context

## Overview
Spring Telescope is a zero-configuration, real-time debugging and observability dashboard for Spring Boot 3 applications. It's packaged as a Spring Boot starter library (not a standalone app).

## Tech Stack
- **Java 17**, **Spring Boot 3.2.5**, **Maven**
- **Lombok** for boilerplate reduction
- **Alpine.js + Tailwind CSS** for the single-page dashboard UI
- No tests currently

## Build
```bash
mvn compile        # Compile
mvn package        # Package JAR
mvn install        # Install to local Maven repo
```

## Project Structure
```
src/main/java/dev/springtelescope/
├── TelescopeAutoConfiguration.java    # Main auto-config (bean definitions)
├── TelescopeProperties.java           # @ConfigurationProperties (prefix: telescope)
├── TelescopeApiResponse.java          # Standard API response wrapper
├── context/                           # User/tenant context (TelescopeUserProvider interface)
├── controller/TelescopeController.java # REST API endpoints
├── filter/                            # Filter provider for dashboard dropdowns
├── model/
│   ├── TelescopeEntry.java            # Core data model (POJO with builder)
│   └── TelescopeEntryType.java        # Enum: REQUEST, QUERY, LOG, SCHEDULE, CACHE, EVENT, MAIL, MODEL, EXCEPTION
├── storage/
│   ├── TelescopeStorage.java          # Storage interface
│   ├── InMemoryTelescopeStorage.java  # Default in-memory (ConcurrentHashMap)
│   └── jpa/                           # Database storage (activated by telescope.storage=database)
│       ├── TelescopeJpaAutoConfiguration.java
│       ├── JpaTelescopeStorage.java   # Buffered JPA impl (ConcurrentLinkedQueue)
│       ├── TelescopeEntryEntity.java  # JPA entity → telescope_entries table
│       ├── TelescopeEntryRepository.java
│       └── TelescopeStorageFlusher.java # Periodic buffer drain
└── watcher/                           # 14 watchers (each auto-detected via @ConditionalOnClass)
    ├── TelescopeRequestFilter.java    # HTTP request/response (Servlet Filter)
    ├── TelescopeContextCaptureFilter.java # Spring Security user extraction
    ├── TelescopeSecurityFilter.java   # Access token protection
    ├── TelescopeQueryInspector.java   # SQL via Hibernate StatementInspector
    ├── TelescopeExceptionHandler.java # @ControllerAdvice
    ├── TelescopeExceptionRecorder.java
    ├── TelescopeLogAppender.java      # Logback appender
    ├── TelescopeScheduleAspect.java   # @Scheduled AOP
    ├── TelescopeCacheAspect.java      # @Cacheable/@CacheEvict AOP
    ├── TelescopeEventWatcher.java     # ApplicationEvent listener
    ├── TelescopeMailWatcher.java      # MailSender AOP
    ├── TelescopeModelListener.java    # Hibernate entity changes
    ├── TelescopeHibernateIntegrator.java # Hibernate SPI
    └── TelescopePruner.java           # Scheduled cleanup
```

## Key Files
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — Auto-config entry point
- `src/main/resources/META-INF/services/org.hibernate.integrator.spi.Integrator` — Hibernate SPI registration
- `src/main/resources/static/telescope/index.html` — Single-page dashboard (Alpine.js, ~1500 lines)

## Architecture Patterns

### Auto-Configuration
- `TelescopeAutoConfiguration` is the main entry point, uses `@ConditionalOnProperty`, `@ConditionalOnClass`, `@ConditionalOnMissingBean`
- Each watcher is individually toggleable via `telescope.watchers.<name>=true/false`
- JPA storage auto-configures via separate `TelescopeJpaAutoConfiguration` when `telescope.storage=database`

### Storage Interface
- `TelescopeStorage` is the core interface: `store()`, `find()`, `findByUuid()`, `clear()`, `prune()`, `getStats()`, `getFilters()`
- Default: `InMemoryTelescopeStorage` (ConcurrentHashMap, CopyOnWriteArrayList, AtomicBoolean)
- Override with `@ConditionalOnMissingBean` — users can provide their own

### Batch Correlation
- `TelescopeBatchContext` (ThreadLocal) assigns a UUID to each HTTP request
- All entries from the same request share the batch ID
- Dashboard "related entries" view uses this

### User Context
- `TelescopeUserProvider` interface with `DefaultTelescopeUserProvider` (reads from Spring Security)
- Multi-tenancy via URL regex pattern (`telescope.tenant-pattern`)

### Dashboard UI
- Single HTML file with embedded Alpine.js app
- 9 views (one per entry type) + home view
- Client-side grouping of duplicate entries (toggle Grouped/Flat)
- Communicates with backend via REST API at `${telescope.base-path}/api/*`

## Configuration Properties (prefix: `telescope`)
| Property | Default | Description |
|----------|---------|-------------|
| enabled | true | Master switch |
| max-entries | 1000 | Max stored entries |
| prune-hours | 24 | Auto-prune age |
| prune-interval-ms | 3600000 | Prune check interval |
| base-path | /telescope | Dashboard URL path |
| base-package | "" | Log filter (package prefix) |
| ignored-prefixes | /actuator,/swagger,/v3/api-docs | Request paths to skip |
| tenant-pattern | "" | Regex for tenant extraction from URL |
| access-token | "" | Token to protect dashboard |
| storage | memory | Storage backend (memory/database) |
| flush-interval-ms | 2000 | DB buffer flush interval |
| watchers.* | true | Individual watcher toggles |

## REST API Endpoints (under `${base-path}/api`)
- `GET /entries` — List entries (params: type, page, size, userIdentifier, tenantId, tag, batchId, search)
- `GET /entries/{uuid}` — Single entry
- `GET /entries/{uuid}/related` — Batch-related entries
- `GET /filters` — Filter dropdown data
- `GET /stats` — Entry count by type
- `GET /status` — Recording status + stats
- `GET /tags` — Distinct tags
- `POST /toggle` — Toggle recording on/off
- `POST /prune?hours=N` — Prune old entries
- `DELETE /entries?type=TYPE` — Clear entries

## Conventions
- All beans are defined in `TelescopeAutoConfiguration` (not component-scanned)
- Watchers are stateless; they receive `TelescopeStorage` and `TelescopeUserProvider` via constructor
- Entry content is stored as `Map<String, Object>` (serialized to JSON for JPA)
- The library MUST NOT pull in transitive dependencies — all starters are `provided` or `optional`
- Sensitive headers (Authorization, Cookie) are masked in request entries
- Body content is truncated to 10KB, stack traces to 5KB
