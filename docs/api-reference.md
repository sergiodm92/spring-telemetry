# REST API Reference

Telescope exposes a REST API under `{telescope.base-path}/api` (default: `/telescope/api`). All endpoints return JSON.

## Response Format

All responses use the `TelescopeApiResponse` wrapper:

```json
{
  "success": true,
  "message": "Human-readable description",
  "data": { ... }
}
```

On errors (e.g., entry not found), standard HTTP status codes are returned without a body.

---

## Endpoints

### List Entries

```
GET /telescope/api/entries
```

Returns paginated entries of a specific type with optional filters.

**Query Parameters:**

| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| `type` | Yes | `String` | — | Entry type: `REQUEST`, `EXCEPTION`, `QUERY`, `LOG`, `SCHEDULE`, `CACHE`, `EVENT`, `MAIL`, `MODEL` |
| `page` | No | `int` | `0` | Page number (zero-based) |
| `size` | No | `int` | `50` | Page size |
| `search` | No | `String` | — | Full-text search across all content values |
| `userIdentifier` | No | `String` | — | Filter by user identifier |
| `tenantId` | No | `String` | — | Filter by tenant ID |
| `method` | No | `String` | — | Filter by HTTP method (REQUEST type only) |
| `statusGroup` | No | `String` | — | Filter by status group: `2xx`, `3xx`, `4xx`, `5xx` (REQUEST type only) |

**Response:**

```json
{
  "success": true,
  "message": "Entries retrieved",
  "data": {
    "entries": [
      {
        "uuid": "a1b2c3d4-...",
        "type": "REQUEST",
        "createdAt": [2025, 1, 15, 14, 30, 45],
        "batchId": "e5f6g7h8-...",
        "content": {
          "method": "GET",
          "uri": "/api/users",
          "status": 200,
          "duration": 45
        },
        "userIdentifier": "user@example.com",
        "tenantId": "42",
        "tags": []
      }
    ],
    "total": 150,
    "page": 0,
    "size": 50
  }
}
```

**Examples:**

```bash
# Get latest requests
curl "http://localhost:8080/telescope/api/entries?type=REQUEST&size=10"

# Get errors only
curl "http://localhost:8080/telescope/api/entries?type=REQUEST&statusGroup=5xx"

# Filter by user
curl "http://localhost:8080/telescope/api/entries?type=REQUEST&userIdentifier=admin@example.com"

# Search in queries
curl "http://localhost:8080/telescope/api/entries?type=QUERY&search=users"

# Filter by tenant and method
curl "http://localhost:8080/telescope/api/entries?type=REQUEST&tenantId=42&method=POST"
```

---

### Get Single Entry

```
GET /telescope/api/entries/{uuid}
```

Returns a single entry by its UUID.

**Response:**

```json
{
  "success": true,
  "message": "Entry retrieved",
  "data": {
    "uuid": "a1b2c3d4-...",
    "type": "REQUEST",
    "createdAt": [2025, 1, 15, 14, 30, 45],
    "batchId": "e5f6g7h8-...",
    "content": { ... },
    "userIdentifier": "user@example.com",
    "tenantId": "42",
    "tags": []
  }
}
```

Returns `404 Not Found` if the entry doesn't exist.

---

### Get Related Entries

```
GET /telescope/api/entries/{uuid}/related
```

Returns all entries that share the same batch ID as the specified entry. This is how the dashboard shows "Related Entries" — all queries, logs, cache operations, etc. that happened during the same HTTP request.

**Response:**

```json
{
  "success": true,
  "message": "Related entries",
  "data": [
    { "uuid": "...", "type": "REQUEST", ... },
    { "uuid": "...", "type": "QUERY", ... },
    { "uuid": "...", "type": "QUERY", ... },
    { "uuid": "...", "type": "LOG", ... }
  ]
}
```

Entries are sorted by `createdAt` ascending.

---

### Get Filter Options

```
GET /telescope/api/filters
```

Returns the options for the dashboard filter dropdowns.

**Response:**

```json
{
  "success": true,
  "message": "Filters",
  "data": {
    "users": [
      { "id": "admin@example.com", "name": "Admin User" },
      { "id": "john@example.com", "name": "John Doe" }
    ],
    "tenants": [
      { "id": "1", "name": "Acme Corp" },
      { "id": "2", "name": "Globex Inc" }
    ],
    "methods": ["GET", "POST", "PUT", "PATCH", "DELETE"],
    "statuses": ["2xx", "3xx", "4xx", "5xx"]
  }
}
```

---

### Get Statistics

```
GET /telescope/api/stats
```

Returns the current entry count per type.

**Response:**

```json
{
  "success": true,
  "message": "Statistics",
  "data": {
    "REQUEST": 245,
    "EXCEPTION": 3,
    "QUERY": 1820,
    "LOG": 567,
    "SCHEDULE": 12,
    "CACHE": 89,
    "EVENT": 34,
    "MAIL": 5,
    "MODEL": 156
  }
}
```

---

### Get Status

```
GET /telescope/api/status
```

Returns whether Telescope is currently enabled and the current stats.

**Response:**

```json
{
  "success": true,
  "message": "Status",
  "data": {
    "enabled": true,
    "stats": {
      "REQUEST": 245,
      "EXCEPTION": 3,
      ...
    }
  }
}
```

---

### Get Tags

```
GET /telescope/api/tags
```

Returns all distinct tags across all entries.

**Response:**

```json
{
  "success": true,
  "message": "Tags",
  "data": [
    "cache:evict",
    "cache:hit",
    "cache:miss",
    "cache:users",
    "entity:User",
    "event:OrderCreated",
    "mail",
    "model:created",
    "model:updated"
  ]
}
```

---

### Toggle Recording

```
POST /telescope/api/toggle
```

Toggles Telescope recording on or off at runtime (without restarting the application).

**Response:**

```json
{
  "success": true,
  "message": "Telescope enabled",
  "data": true
}
```

The `data` field contains the new enabled state (`true` or `false`).

---

### Prune Entries

```
POST /telescope/api/prune?hours={hours}
```

Removes all entries older than the specified number of hours.

**Query Parameters:**

| Parameter | Required | Type | Default | Description |
|-----------|----------|------|---------|-------------|
| `hours` | No | `int` | `24` | Remove entries older than this many hours |

**Response:**

```json
{
  "success": true,
  "message": "Pruned 42 entries",
  "data": {
    "pruned": 42,
    "hours": 24
  }
}
```

---

### Clear Entries

```
DELETE /telescope/api/entries?type={TYPE}
```

Clears all entries. If `type` is specified, only entries of that type are cleared.

**Query Parameters:**

| Parameter | Required | Type | Description |
|-----------|----------|------|-------------|
| `type` | No | `String` | Entry type to clear. If omitted, clears ALL entries. |

**Response:**

```json
{
  "success": true,
  "message": "Entries cleared",
  "data": null
}
```

**Examples:**

```bash
# Clear all entries
curl -X DELETE "http://localhost:8080/telescope/api/entries"

# Clear only queries
curl -X DELETE "http://localhost:8080/telescope/api/entries?type=QUERY"

# Clear only logs
curl -X DELETE "http://localhost:8080/telescope/api/entries?type=LOG"
```

---

## Entry Content by Type

Each entry type has a different `content` structure:

### REQUEST

```json
{
  "method": "POST",
  "uri": "/api/users",
  "queryString": null,
  "status": 201,
  "duration": 45,
  "ipAddress": "127.0.0.1",
  "contentType": "application/json",
  "responseContentType": "application/json",
  "requestHeaders": { "host": "localhost:8080", "authorization": "***" },
  "responseHeaders": { "content-type": "application/json" },
  "requestBody": "{\"name\": \"John\"}",
  "responseBody": "{\"id\": 1, \"name\": \"John\"}"
}
```

### EXCEPTION

```json
{
  "class": "java.lang.NullPointerException",
  "message": "Cannot invoke method on null",
  "trace": "java.lang.NullPointerException...",
  "file": "UserService.java",
  "line": 42,
  "location": "com.myapp.service.UserService.findUser",
  "uri": "/api/users/1",
  "method": "GET",
  "cause": "java.sql.SQLException: Connection refused"
}
```

### QUERY

```json
{
  "sql": "select u1_0.id, u1_0.email from users u1_0 where u1_0.id=?",
  "type": "SELECT"
}
```

### LOG

```json
{
  "level": "ERROR",
  "message": "Failed to process order",
  "logger": "com.myapp.service.OrderService",
  "thread": "http-nio-8080-exec-1",
  "exception": "java.lang.RuntimeException: ...",
  "mdc": { "requestId": "abc-123" }
}
```

### SCHEDULE

```json
{
  "class": "OrderCleanupTask",
  "method": "cleanupExpiredOrders",
  "status": "completed",
  "duration": 1523,
  "exception": null
}
```

### CACHE

```json
{
  "operation": "HIT",
  "cacheName": "users",
  "key": "UserService.findById(..) [42]",
  "duration": 0
}
```

### EVENT

```json
{
  "event": "OrderCreatedEvent",
  "eventClass": "com.myapp.events.OrderCreatedEvent",
  "source": "OrderService",
  "payload": { "orderId": "123", "amount": "99.99" }
}
```

### MAIL

```json
{
  "to": "user@example.com",
  "from": "noreply@myapp.com",
  "subject": "Welcome",
  "bodyPreview": "Hello John..."
}
```

### MODEL

```json
{
  "entity": "User",
  "entityClass": "com.myapp.model.User",
  "action": "UPDATED",
  "entityId": "42",
  "changedFields": ["email", "updatedAt"]
}
```
