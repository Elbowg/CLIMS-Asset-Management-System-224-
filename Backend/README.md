
# 🔧 Backend — CLIMS (Spring Boot + Java)

[![CI](https://github.com/Elbowg/CLIMS-Asset-Management-System-224-/actions/workflows/ci.yml/badge.svg)](https://github.com/Elbowg/CLIMS-Asset-Management-System-224-/actions/workflows/ci.yml)

This folder contains the **backend logic** for the Computer & Laptop Asset Management System (CLIMS).

---

## ⚡ Quick Start (PowerShell)

**Easiest way to run the backend on Windows:**

```powershell
# Navigate to the Backend folder
cd Backend

# Run the startup script
.\start-backend.ps1
```

The script will:
- Navigate to the backend module directory
- Start the Spring Boot application using Maven
- Display startup progress

**Wait for these lines:**
```
Tomcat started on port(s): 8080
Started BackendApplication
```

Once you see those, the backend is ready at `http://localhost:8080` 🎉

**Test it's working:**
```powershell
# Check health
Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health'

# Test the hello endpoint (requires authentication in secured mode)
Invoke-RestMethod -Uri 'http://localhost:8080/api/hello'
```

---

## ✅ Operational Features
| Feature | Status | Details |
|---------|--------|---------|
| Flyway Migrations | ✔ | Deterministic user + role seeding through V15 |
| JWT Auth (Access + Refresh) | ✔ | Stateless, roles in access token only |
| Role-Based Access Control | ✔ | `user_roles` join table, ADMIN / USER |
| Custom 401/403 Handling | ✔ | JSON error responses via custom handlers |
| Actuator Health/Info | ✔ | `/actuator/health`, `/actuator/info` (permitted) |
| OpenAPI Docs | ✔ | `/v3/api-docs`, `/swagger-ui/index.html` |
| Docker Image | ✔ | Multi-stage Dockerfile + healthcheck |
| Externalized Config | ✔ | Env var overrides for DB + JWT |
| Graceful Shutdown | ✔ | Enabled in prod profile |

## 🚀 Run (Local Dev)
Prerequisites: **Java 17**, Maven Wrapper included.

### Standard Startup (Recommended)

```powershell
# Windows PowerShell
cd Backend\backend
.\mvnw.cmd spring-boot:run
```

```bash
# Linux/Mac
cd Backend/backend
./mvnw spring-boot:run
```

**Default configuration:**
- Uses H2 in-memory database (auto-configured via `local` profile)
- Runs on port 8080
- JWT authentication enabled
- Swagger UI available at http://localhost:8080/swagger-ui/index.html

### Getting Started with Authentication

The backend uses JWT authentication. To call protected endpoints:

**PowerShell:**
```powershell
# Login (seeded users: admin/admin or user/user)
$loginBody = @{ username = 'admin'; password = 'admin' } | ConvertTo-Json
$tokens = Invoke-RestMethod -Method POST -Uri 'http://localhost:8080/api/auth/login' -Body $loginBody -ContentType 'application/json'

# Use the access token
$headers = @{ Authorization = "Bearer $($tokens.accessToken)" }
Invoke-RestMethod -Headers $headers -Uri 'http://localhost:8080/api/hello'
```

**Or use Swagger UI (easier):**
1. Open http://localhost:8080/swagger-ui/index.html
2. Click "Authorize"  
3. Login at `/api/auth/login` to get a token
4. Paste `Bearer YOUR_ACCESS_TOKEN` in the authorization dialog
5. Try endpoints with authentication

### Alternative: Insecure Dev Mode

For rapid frontend development without dealing with JWTs:

```powershell
cd Backend\backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local,insecure
```

**Features:**
- No authentication required for API calls
- CORS enabled for frontend development
- Bound to localhost (127.0.0.1) only for safety
- **⚠️ WARNING**: Only for local development, never in production

### Advanced Options

Override JWT secret & expirations:
```bash
JWT_SECRET=changeMeStrong JWT_ACCESS_EXPIRATION=7200000 ./mvnw spring-boot:run
```

Run the built JAR:
```bash
./mvnw -q package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## 🌐 Important Endpoints
| Purpose | Method | Path |
|---------|--------|------|
| Authenticate (login) | POST | `/api/auth/login` |
| Refresh token | POST | `/api/auth/refresh` |
| List assets | GET | `/api/assets` |
| Health | GET | `/actuator/health` |
| OpenAPI JSON | GET | `/v3/api-docs` |
| Swagger UI | GET | `/swagger-ui/index.html` |

### Admin ETL (Reporting)
Admin-only endpoints to backfill daily summary tables (idempotent delete-then-insert). See full details in `backend/Docs/REPORTING_API.md`.

| Purpose | Method | Path |
|---------|--------|------|
| Backfill maintenance activity daily | POST | `/api/admin/etl/maintenance-activity-daily` |
| Backfill asset status daily | POST | `/api/admin/etl/asset-status-daily` |
| Backfill audit action daily | POST | `/api/admin/etl/audit-action-daily` |

## 🔐 Environment Variables
| Variable | Default (local) | Notes |
|----------|-----------------|-------|
| `JWT_SECRET` | `devInsecureChangeMe` | MUST override in prod |
| `JWT_ACCESS_EXPIRATION` | `86400000` | ms (24h) |
| `JWT_REFRESH_EXPIRATION` | `2592000000` | ms (30d) |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/clims_db` | Prod override |
| `DB_USERNAME` | `clims_user` | |
| `DB_PASSWORD` | `password123` | Change in prod |
| `SPRING_PROFILES_ACTIVE` | `local` | Use `prod` for production |

## 🧪 Testing
```bash
./mvnw test
```
Integration tests spin up MySQL via Testcontainers and run full Flyway migrations.

## � Pagination Policy (Assets Endpoint)
`GET /api/assets` now returns a Spring `Page` of lightweight projection rows (id, name, status, assignedUserId) instead of fully materialized entities.

| Parameter | Default | Max | Notes |
|-----------|---------|-----|-------|
| `page` | 0 | — | Zero-based page index |
| `size` | 20 | 100 | Values >100 are capped to protect DB & memory |

### Response Shape Example
```json
{
   "content": [ {"id":1,"name":"Asset-1","status":"ACTIVE","assignedUserId":5}, ... ],
   "pageable": {"pageNumber":0,"pageSize":20,...},
   "totalElements": 412,
   "totalPages": 21,
   "size": 20,
   "number": 0,
   "numberOfElements": 20,
   "first": true,
   "last": false
}
```

### Rationale
1. Prevent unbounded result sets (production safety & latency).
2. Reduce memory & serialization overhead via projection (only required columns are selected by JPA provider when using interface projection).
3. Provide consistent API contract for future filtering/sorting.

### Backward Compatibility
If any client depended on the previous plain JSON array, they must now read from the `content` field. (If strict backwards compatibility is required, we can introduce a `?legacy=true` query param returning the old shape temporarily.)

### Filters & Sorting (Now Available)
The assets endpoint supports optional filtering and whitelisted sorting.

| Query Param | Type | Example | Behavior |
|-------------|------|---------|----------|
| `status` | Enum (`ACTIVE`,`INACTIVE`, etc.) | `status=ACTIVE` | Filters to a single status (ignored if blank/invalid) |
| `assignedUserId` | Long | `assignedUserId=42` | Returns only assets assigned to the given user ID |
| `sort` | `<field>(,asc|,desc)` | `sort=name,asc` / `sort=id,desc` | Whitelisted fields only; defaults to `id,asc` |

Whitelisted sort fields: `id`, `name`, `status`, `assignedUserId`.

Invalid or non-whitelisted sort directives silently fall back to `id,asc` for safety and to avoid leaking internal columns.

Combined usage example:
```
/api/assets?status=ACTIVE&assignedUserId=42&page=0&size=50&sort=name,asc
```

#### Projection Notes
The endpoint returns interface projections so only the selected columns are fetched (`asset_id`, `name`, `status`, `assigned_user_id`). This reduces I/O vs. full entity hydration. A forthcoming doc will show a side‑by‑side SQL comparison and estimated bandwidth reduction.

#### Error Handling
- An invalid `status` value is ignored (treated as null filter) rather than returning 400 — keeps the API lenient for exploratory UI usage.
- Oversized `size` values are clamped to 100 (documented above).

### Future Enhancements
- Keyset pagination for very large tables.
- ETag / conditional requests for stable listing windows.
- Multi-field sort parsing (e.g., `sort=name,asc;status,desc`).
- Caching layer & N+1 query guard (in progress in Phase 4 roadmap).


## �🐳 Docker
Build image:
```bash
docker build -t clims-backend:latest .
```
Run container (prod profile):
```bash
docker run -p 8080:8080 \
   -e SPRING_PROFILES_ACTIVE=prod \
   -e JWT_SECRET=superSecretValue \
   -e DB_URL=jdbc:mysql://mysql:3306/clims_db \
   -e DB_USERNAME=clims_user \
   -e DB_PASSWORD=changeMe \
   clims-backend:latest
```
Health check:
```bash
curl -f http://localhost:8080/actuator/health
```

### 📦 GHCR (GitHub Container Registry)
Images are published automatically from the `main` branch and tags to:

- `ghcr.io/elbowg/clims-backend:latest`
- `ghcr.io/elbowg/clims-backend:<git-sha>`
- `ghcr.io/elbowg/clims-backend:<tag>` (when pushing tags like v1.2.3)

Pull and run from GHCR:
```bash
docker pull ghcr.io/elbowg/clims-backend:latest
docker run -p 8080:8080 \
   -e SPRING_PROFILES_ACTIVE=prod \
   -e JWT_SECRET=superSecretValue \
   -e DB_URL=jdbc:mysql://mysql:3306/clims_db \
   -e DB_USERNAME=clims_user \
   -e DB_PASSWORD=changeMe \
   ghcr.io/elbowg/clims-backend:latest
```

## 🧩 Profiles
| Profile | Intent | DB | Notes |
|---------|--------|----|-------|
| `local` | Dev convenience | MySQL (manual) | H2 used in some tests |
| `test` | Automated tests | Testcontainers MySQL + H2 | Activated by test framework |
| `prod` | Deployment | External MySQL | Limited actuator exposure |

## 🛡 Security Model (Summary)
1. Access token (short-ish lived) carries roles.
2. Refresh token only for renewal — misuse & wrong type tested.
3. 401 → unauthenticated/expired/invalid; 403 → authenticated but forbidden.

## 🛠 Graceful Shutdown
Configured via `server.shutdown=graceful` and `spring.lifecycle.timeout-per-shutdown-phase=30s` (prod only).

## 📘 OpenAPI
After startup visit: http://localhost:8080/swagger-ui/index.html

## 📊 Metrics (Micrometer)
All metrics exposed via Spring Boot Actuator (e.g. `/actuator/metrics`, Prometheus scraper if enabled).

### Auth & Security Counters
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `auth.login.success` | counter | — | Successful login attempts |
| `auth.logout.success` | counter | — | Successful logout (refresh token invalidated) |
| `auth.logout.invalid` | counter | — | Logout request with missing/invalid token |
| `auth.refresh.success` | counter | — | Successful refresh (rotation executed) |
| `auth.refresh.invalid` | counter | — | Refresh request with malformed/invalid token |
| `auth.refresh.blacklisted` | counter | — | Refresh attempt using blacklisted token |
| `auth.refresh.rotate` | counter | — | Refresh rotations performed (new pair issued) |
| `auth.blacklist.hit` | counter | — | Blacklisted token encountered |
| `auth.blacklist.miss` | counter | — | Non-blacklisted token encountered during refresh |
| `auth.blacklist.add` | counter | flow=logout|refresh | Blacklist insertions (token revocations) |
| `auth.access.issued` | counter | flow=login|refresh | Access tokens generated |
| `auth.refresh.issued` | counter | flow=login|refresh | Refresh tokens generated |
| `auth.blacklist.size` | gauge | — | Current in-memory blacklist size (dev only) |

### Audit Metrics
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `audit.write.success` | counter | — | Persisted audit events |
| `audit.write.failure` | counter | exception | Failed audit writes (increments before exception propagates) |
| `audit.retention.purged` | counter | retentionDays | Rows deleted by retention job run |

### Domain Metrics
| Metric | Type | Tags | Description |
| `domain.assets.count` | gauge | status | Asset count per status refreshed every 30s |
| `domain.maintenance.total` | gauge | — | Total maintenance records |

Enable Prometheus export by adding dependency `micrometer-registry-prometheus` and setting:
```
management.endpoints.web.exposure.include=health,info,metrics,prometheus
```

## 📨 Outbox & Eventing (Phase 3 Foundation + Handler Registry)
Implements a transactional outbox pattern; now enhanced with a pluggable handler registry.

### Data Model
`outbox_event` (Flyway V18) core fields:
`aggregate_type`, `aggregate_id`, `event_type`, `payload` (JSON), `status` (NEW|RETRY|SENT|FAILED), `attempt_count`, `next_attempt_at`, correlation / request IDs, timestamps.

### Dispatch Flow
1. Domain layer (e.g. `AssetService#create`) records an event inside the main transaction.
2. `OutboxDispatcher` polls due rows (NEW/RETRY whose `next_attempt_at` is null or <= now).
3. Looks up the event’s `event_type` in a handler map (eventType → `OutboxEventHandler`).
4. Handler executes business side-effect (currently logging via `AssetCreatedHandler`).
5. Success → mark SENT. Exception → RETRY with exponential backoff. Exceeding `maxAttempts` → FAILED. Missing handler → immediate FAILED.

### Handler Contract
```java
public interface OutboxEventHandler {
   String getEventType();
   void handle(OutboxEvent event) throws Exception; // throw to trigger retry
}
```
Registered handler(s):
| Event Type | Handler | Notes |
|------------|---------|-------|
| `AssetCreated` | `AssetCreatedHandler` | Placeholder (logs); future: emit integration event |

### Metrics
| Metric | Type | Description |
|--------|------|-------------|
| `outbox.insert.success` | counter | Successful persisted events |
| `outbox.insert.failure` | counter | Failed persists (tagged) |
| `outbox.dispatch.attempt` | counter | Events fetched in a poll batch |
| `outbox.dispatch.success` | counter | Marked SENT |
| `outbox.dispatch.failure` | counter | Handler threw (tagged) |
| `outbox.dispatch.dead` | counter | Permanently FAILED (max attempts) |
| `outbox.dispatch.missingHandler` | counter | No handler mapped for event type |
| `outbox.queue.depth` | gauge(status) | Per-status counts |
| `outbox.dispatch.batch.time` | timer | Batch processing duration |

### Configuration (Defaults)
| Property | Default | Purpose |
|----------|---------|---------|
| `outbox.dispatch.interval-ms` | 5000 | Fixed delay scheduler interval |
| `outbox.dispatch.max-attempts` | 5 | Attempts before FAILED |
| `outbox.dispatch.initial-backoff-ms` | 500 | First retry delay |
| `outbox.dispatch.backoff-multiplier` | 2.0 | Exponential multiplier |
| `outbox.dispatch.batch-size` | 50 | (Reserved) Planned query limit |

### Failure Semantics
| Condition | Result | Metric(s) |
|-----------|--------|-----------|
| Handler success | SENT | `outbox.dispatch.success` |
| Handler exception | RETRY + backoff | `outbox.dispatch.failure` |
| Max attempts exceeded | FAILED | `outbox.dispatch.dead` |
| No handler | FAILED | `outbox.dispatch.missingHandler` |

### Upcoming Enhancements
- Multi-handler fanout (one event → many side effects)
- Idempotency key + duplicate suppression metric (`outbox.duplicate`)
- Admin dead-letter (FAILED) requeue endpoint & paging API
- External broker strategy (Kafka/RabbitMQ) adapter layer
- Purge job for aging SENT/FAILED rows (retention window)

Events currently provide an auditable record of domain changes; downstream publishing can evolve without altering domain transactions.

## �🔍 Tracing & Correlation
The backend standardizes two identifiers that help you follow a request end‑to‑end:

| Field | Origin | Propagation | Surfaces In |
|-------|--------|-------------|-------------|
| `requestId` | Always generated server-side (UUID/short) at filter entry | Not forwarded downstream (scoped to this service) | Error responses, (optionally) audit events, logs |
| `correlationId` | Incoming header `X-Correlation-Id` if present; otherwise generated | Echoed back in response header & payload; forwarded on outbound calls (future) | Error responses, logs |

### How It Works
1. An inbound filter assigns / preserves IDs.
2. Controllers / exception handler include them in the standardized JSON error model.
3. Audit writes include at least the `requestId` (correlation may be added in a future migration if needed for cross-service stitching).

### Sample Error Payload
```json
{
   "timestamp": "2025-10-08T12:34:56.789Z",
   "status": 403,
   "error": "FORBIDDEN",
   "message": "Access denied",
   "path": "/api/assets",
   "requestId": "6f3a0c7d9fbe4d0a",
   "correlationId": "a1d2e3f4-9b8c-47d0-8a0e-1c2b3d4e5f60"
}
```

### Operational Flow
| Step | Action | Operator Guidance |
|------|--------|-------------------|
| 1 | User reports failure with IDs | Ask for both IDs from client response |
| 2 | Search logs by `requestId` | Fast, uniquely identifies single request |
| 3 | (Optional) Group related requests by `correlationId` | Useful once multiple services adopt same header |
| 4 | Inspect metrics (e.g. auth.* counters) around timestamp | Validate if systemic vs isolated |
| 5 | (If security/audit) Query `audit_event` by timestamp+principal | Correlate security actions |

### Headers
| Name | Direction | Description |
|------|-----------|-------------|
| `X-Correlation-Id` | In/Out | Client may supply; otherwise server creates |
| `X-Request-Id` | Out | Mirrors internal `requestId` (optional to expose) |

### Troubleshooting Checklist
- Spike in `auth.refresh.invalid`? Examine sample failing requests via shared `correlationId`.
- `audit.write.failure` increments? Use `requestId` in logs around failure time for stack traces.
- Missing IDs in a response? Verify filter registration order and exception mapping (ensure global handler used).

### Future Enhancements
- Persist `correlationId` in `audit_event` (add nullable column via migration if cross-service tracing becomes critical).
- Emit OpenTelemetry trace/span with IDs as attributes once tracing backend (Jaeger/OTel Collector) is introduced.

## ▶ Suggested Next Steps
- Add Prometheus metrics + readiness probe
- Add structured logging + correlation IDs
- Implement refresh token rotation & revocation store
- Introduce CI pipeline (build/test/security scan/docker push)
- Add OpenTelemetry tracing


