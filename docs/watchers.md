# Watchers Guide

Telescope uses **watchers** to observe different aspects of your application. Each watcher is a Spring bean that is conditionally registered based on classpath dependencies and configuration.

## Overview

| Watcher | Class | Mechanism | Dependencies |
|---------|-------|-----------|-------------|
| Requests | `TelescopeRequestFilter` | Servlet Filter | `spring-boot-starter-web` |
| Context Capture | `TelescopeContextCaptureFilter` | Servlet Filter | `spring-boot-starter-web` |
| Queries | `TelescopeQueryInspector` | Hibernate `StatementInspector` | `spring-boot-starter-data-jpa` |
| Exceptions | `TelescopeExceptionRecorder` | Injectable service | Core |
| Logs | `TelescopeLogAppender` | Logback `AppenderBase` | `logback-classic` |
| Schedules | `TelescopeScheduleAspect` | AOP `@Around` | `spring-boot-starter-aop` |
| Cache | `TelescopeCacheAspect` | AOP `@Around` | `spring-boot-starter-aop` |
| Events | `TelescopeEventWatcher` | `@EventListener` | Core |
| Mail | `TelescopeMailWatcher` | AOP `@Around` | `spring-boot-starter-aop` + `spring-boot-starter-mail` |
| Models | `TelescopeModelListener` | Hibernate `Integrator` SPI | `spring-boot-starter-data-jpa` |
| Pruner | `TelescopePruner` | `@Scheduled` | Core |

---

## Request Watcher

### How it works

Two servlet filters work together:

1. **`TelescopeRequestFilter`** (Order: `HIGHEST_PRECEDENCE + 10`) — Wraps the request/response in `ContentCachingRequestWrapper`/`ContentCachingResponseWrapper`, sets up a batch ID, measures duration, and records the full request/response after the chain completes.

2. **`TelescopeContextCaptureFilter`** (Order: `2`) — Runs inside the Spring Security filter chain where the `SecurityContext` is available. Captures user identity and tenant ID as request attributes. The request filter reads these attributes in its `finally` block (where SecurityContext may already be cleared).

### What is captured

```json
{
  "method": "POST",
  "uri": "/api/users",
  "queryString": "page=0&size=10",
  "status": 201,
  "duration": 45,
  "ipAddress": "127.0.0.1",
  "contentType": "application/json",
  "responseContentType": "application/json",
  "requestHeaders": { "host": "localhost:8080", "authorization": "***" },
  "responseHeaders": { "content-type": "application/json" },
  "requestBody": "{\"name\": \"John\", \"email\": \"john@example.com\"}",
  "responseBody": "{\"id\": 1, \"name\": \"John\"}"
}
```

### Security measures

- `Authorization` and `Cookie` headers are masked with `***`
- Request and response bodies are truncated at 10,000 characters
- The telescope base path is automatically excluded from recording

### Configuring ignored paths

```yaml
telescope:
  ignored-prefixes:
    - /actuator
    - /swagger
    - /v3/api-docs
    - /static
```

---

## Query Watcher

### How it works

Uses Hibernate's `StatementInspector` SPI, registered via `HibernatePropertiesCustomizer`. The inspector sees every SQL statement before execution and records it without modifying the SQL.

### What is captured

```json
{
  "sql": "select u1_0.id, u1_0.email, u1_0.name from users u1_0 where u1_0.id=?",
  "type": "SELECT"
}
```

SQL type is classified as: `SELECT`, `INSERT`, `UPDATE`, `DELETE`, `DDL`, or `OTHER`.

### Excluded queries

Health-check queries are automatically excluded:
- `SELECT 1`
- `SELECT VERSION`

---

## Exception Watcher

### How it works

Unlike other watchers, the exception recorder is a **service bean** that you invoke manually from your exception handlers. This design is intentional — it gives you full control over which exceptions are recorded.

### Integration

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private final TelescopeExceptionRecorder recorder;

    public GlobalExceptionHandler(TelescopeExceptionRecorder recorder) {
        this.recorder = recorder;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex) {
        recorder.record(ex);
        return ResponseEntity.status(500).build();
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusiness(BusinessException ex) {
        // Choose NOT to record business exceptions
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
```

### What is captured

```json
{
  "class": "java.lang.NullPointerException",
  "message": "Cannot invoke method on null object",
  "trace": "java.lang.NullPointerException: ...\n\tat com.myapp...",
  "file": "UserService.java",
  "line": 42,
  "location": "com.myapp.service.UserService.findUser",
  "uri": "/api/users/1",
  "method": "GET",
  "cause": "java.sql.SQLException: Connection refused"
}
```

Stack traces are truncated at 5,000 characters.

---

## Log Watcher

### How it works

Attaches a custom Logback `AppenderBase<ILoggingEvent>` to the root logger during initialization. The appender receives all log events and filters them based on level and package.

### Filtering rules

1. **Level:** Only `INFO`, `WARN`, and `ERROR` are captured (DEBUG and TRACE are excluded)
2. **Package:** If `telescope.base-package` is set, only logs from that package are captured
3. **Self-exclusion:** Logs from `dev.springtelescope.*` are always excluded (prevents infinite loops)

### What is captured

```json
{
  "level": "ERROR",
  "message": "Failed to process order #123",
  "logger": "com.myapp.service.OrderService",
  "thread": "http-nio-8080-exec-1",
  "exception": "java.lang.RuntimeException: Payment failed\n\tat ...",
  "mdc": { "requestId": "abc-123", "userId": "user@example.com" }
}
```

---

## Schedule Watcher

### How it works

An AOP `@Around` aspect that intercepts all methods annotated with `@Scheduled`. Each execution gets its own batch ID.

### What is captured

```json
{
  "class": "OrderCleanupTask",
  "method": "cleanupExpiredOrders",
  "status": "completed",
  "duration": 1523,
  "exception": null
}
```

Status is either `completed` or `failed`. On failure, the exception message is included and the original exception is re-thrown.

---

## Cache Watcher

### How it works

AOP aspects around `@Cacheable`, `@CacheEvict`, and `@CachePut` annotations. Detects cache hits vs misses using a duration heuristic: executions under 2ms are classified as hits.

### What is captured

```json
{
  "operation": "HIT",
  "cacheName": "users",
  "key": "UserService.findById(..) [42]",
  "duration": 0
}
```

Operations: `HIT`, `MISS`, `EVICT`, `PUT`.

---

## Event Watcher

### How it works

Uses `@EventListener` to listen for all `ApplicationEvent` instances. Payload extraction uses reflection to call getter methods on the event object.

### Filtering rules

1. **Package:** If `telescope.base-package` is set, only events from that package are captured
2. **Framework exclusion:** Events from `org.springframework.*` are excluded
3. **Self-exclusion:** Events from `dev.springtelescope.*` are excluded

### What is captured

```json
{
  "event": "OrderCreatedEvent",
  "eventClass": "com.myapp.events.OrderCreatedEvent",
  "source": "OrderService",
  "payload": {
    "orderId": "123",
    "totalAmount": "99.99"
  }
}
```

---

## Mail Watcher

### How it works

AOP aspect around `MailSender.send()` method executions. Captures the `SimpleMailMessage` details before the send proceeds.

### What is captured

```json
{
  "to": "user@example.com",
  "from": "noreply@myapp.com",
  "subject": "Welcome to MyApp",
  "bodyPreview": "Hello John, welcome to our platform..."
}
```

Body is truncated at 500 characters.

---

## Model Watcher

### How it works

Registered as a Hibernate `Integrator` via `META-INF/services/org.hibernate.integrator.spi.Integrator`. The integrator registers `TelescopeModelListener` as a `PostInsertEventListener`, `PostUpdateEventListener`, and `PostDeleteEventListener`.

### What is captured

```json
{
  "entity": "User",
  "entityClass": "com.myapp.model.User",
  "action": "UPDATED",
  "entityId": "42",
  "changedFields": ["email", "updatedAt"]
}
```

Actions: `CREATED`, `UPDATED`, `DELETED`. Changed fields are detected by comparing old and new state arrays from Hibernate.

---

## Pruner

### How it works

A `@Scheduled` bean that runs at a fixed rate (default: every hour) and removes entries older than the configured threshold (default: 24 hours).

### Configuration

```yaml
telescope:
  prune-hours: 12          # Remove entries older than 12 hours
  prune-interval-ms: 600000 # Run every 10 minutes
```

You can also trigger pruning manually via the API:

```bash
curl -X POST http://localhost:8080/telescope/api/prune?hours=6
```

---

## Batch Correlation

All watchers use `TelescopeBatchContext` (a `ThreadLocal<String>`) to associate entries with the same HTTP request. When the Request Watcher starts processing a request, it generates a UUID and stores it in the batch context. All other watchers read this context and attach the same batch ID to their entries.

This enables the "Related Entries" feature in the dashboard — clicking on any entry shows all other entries from the same request (queries, logs, cache operations, etc.).

For scheduled tasks, the Schedule Watcher creates its own batch ID, so all entries generated during a scheduled execution are grouped together.
