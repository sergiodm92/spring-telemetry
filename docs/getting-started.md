# Getting Started with Spring Telescope

This guide walks you through adding Spring Telescope to your existing Spring Boot 3 application.

## Prerequisites

- Java 17 or higher
- Spring Boot 3.x application
- Maven or Gradle

## Step 1: Add the Dependency

### Maven

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.springtelescope</groupId>
    <artifactId>spring-telescope</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```groovy
implementation 'dev.springtelescope:spring-telescope:0.1.0'
```

## Step 2: Configure Your Base Package

While Telescope works with zero configuration, setting your base package is highly recommended. It ensures that only your application's logs and events are captured (instead of all framework logs):

```yaml
# application.yml
telescope:
  base-package: com.mycompany.myapp
```

Or in properties format:

```properties
# application.properties
telescope.base-package=com.mycompany.myapp
```

## Step 3: Start Your Application

Start your Spring Boot application normally:

```bash
mvn spring-boot:run
```

You should see Telescope's auto-configuration activating in the logs. Open your browser and navigate to:

```
http://localhost:8080/telescope
```

## Step 4: Verify It Works

1. **Open the dashboard** — you should see the main overview page with stat cards
2. **Make some API calls** to your application — they should appear under "Requests"
3. **Check the sidebar** — entry counts update in real-time (auto-refresh every 5 seconds)

## Step 5: Integrate Exception Recording (Optional)

The exception watcher provides a `TelescopeExceptionRecorder` bean. To record exceptions, inject it into your global exception handler:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private final TelescopeExceptionRecorder recorder;

    public GlobalExceptionHandler(TelescopeExceptionRecorder recorder) {
        this.recorder = recorder;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        recorder.record(ex);  // Record in Telescope
        return ResponseEntity.status(500)
            .body(Map.of("error", ex.getMessage()));
    }
}
```

## What Gets Captured Automatically

With the default configuration and typical Spring Boot dependencies, you'll get:

| What | Automatic? | Dependency Needed |
|------|-----------|-------------------|
| HTTP Requests | Yes | `spring-boot-starter-web` (always present) |
| SQL Queries | Yes | `spring-boot-starter-data-jpa` |
| Application Logs | Yes | `logback-classic` (comes with starter-web) |
| Scheduled Tasks | Yes | `spring-boot-starter-aop` |
| Cache Operations | Yes | `spring-boot-starter-aop` |
| Application Events | Yes | None (core Spring) |
| Model Changes | Yes | `spring-boot-starter-data-jpa` |
| Exceptions | Manual | None (inject `TelescopeExceptionRecorder`) |
| Mail | Yes | `spring-boot-starter-aop` + `spring-boot-starter-mail` |

## Next Steps

- [Configuration Reference](configuration.md) — full list of properties
- [Watchers Guide](watchers.md) — detailed info on each watcher
- [Customization Guide](customization.md) — user providers, filter providers, storage
- [REST API Reference](api-reference.md) — programmatic access to Telescope data
- [Security Guide](security.md) — securing the dashboard in production
