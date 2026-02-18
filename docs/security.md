# Security Guide

Spring Telescope is primarily a **development and debugging tool**. It exposes detailed information about your application's internals, so it's important to secure it properly.

## Built-in Security Measures

Telescope includes several security features out of the box:

### Header Masking

Sensitive HTTP headers are automatically masked:

```
Authorization: ***
Cookie: ***
```

### Body Truncation

Large request/response bodies are truncated to prevent memory issues:

- Request body: 10,000 characters max
- Response body: 10,000 characters max
- Stack traces: 5,000 characters max
- Mail body preview: 500 characters max

### Self-Exclusion

Telescope automatically excludes its own traffic:

- The `telescope.base-path` is added to `ignored-prefixes`
- Telescope's own logs (`dev.springtelescope.*`) are excluded from the log watcher
- Telescope's own events and entities are excluded from their respective watchers

### Fail-Safe Design

All watchers catch and swallow their own exceptions. Telescope will never break your application — if a watcher fails, it silently skips the entry.

---

## Securing the Dashboard

### Option 1: Disable in Production

The simplest approach — completely disable Telescope in production:

```yaml
# application-prod.yml
telescope:
  enabled: false
```

When disabled, no beans are created and no memory is consumed.

### Option 2: Restrict with Spring Security

If you want Telescope available in production (e.g., for debugging), restrict access to administrators:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // Restrict Telescope to admin users
                .requestMatchers("/telescope/**").hasRole("ADMIN")
                // Your other rules...
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

### Option 3: Network-Level Restriction

Use your reverse proxy (Nginx, Apache, cloud load balancer) to block external access:

```nginx
# Nginx example
location /telescope {
    allow 10.0.0.0/8;    # Internal network only
    deny all;
}
```

### Option 4: Custom Base Path

Move Telescope to a less discoverable path:

```yaml
telescope:
  base-path: /internal/debug/telescope-a7x9k2
```

**Note:** Security through obscurity alone is not sufficient, but it adds a layer when combined with other measures.

---

## CSRF Considerations

If your application has CSRF protection enabled and you want to use Telescope's POST/DELETE endpoints (toggle, prune, clear) from the dashboard, the dashboard uses `fetch()` which doesn't automatically include CSRF tokens.

Options:

1. **Exclude Telescope from CSRF** (recommended for dev tools):

```java
http.csrf(csrf -> csrf
    .ignoringRequestMatchers("/telescope/api/**")
)
```

2. **Disable CSRF for the entire API** (if your API is stateless/JWT-based, you likely already do this).

---

## Data Sensitivity

### What Telescope captures

Be aware of the data Telescope stores in memory:

- **Request/response bodies** — may contain PII, passwords, tokens
- **SQL queries** — may reveal database schema and parameter patterns
- **Log messages** — may contain sensitive data depending on your logging
- **Exception stack traces** — may reveal internal code structure
- **Mail content** — subject, recipients, body preview

### Recommendations

1. **Don't use Telescope in production with sensitive data** unless access is strictly controlled
2. **Set `max-entries` conservatively** to limit how much data is held in memory
3. **Use `prune-hours`** to automatically clean up old entries
4. **Disable watchers you don't need** to reduce the attack surface:

```yaml
telescope:
  watchers:
    mail: false    # Don't capture mail content
    models: false  # Don't capture entity changes
```

4. **Consider your `base-package`** — a narrow base package limits what logs and events are captured

---

## Memory Considerations

Telescope stores all entries in memory. The maximum memory usage is bounded by:

```
Max memory = 9 types x max-entries x average entry size
```

With default settings (1000 entries per type), typical memory usage is 10-50 MB depending on entry content (request/response bodies being the largest contributors).

To reduce memory usage:

```yaml
telescope:
  max-entries: 200      # Reduce entries per type
  prune-hours: 6        # Prune more aggressively
  prune-interval-ms: 600000  # Prune every 10 minutes
```
