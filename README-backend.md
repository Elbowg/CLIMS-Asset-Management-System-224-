# CLIMS Backend (Spring Boot)

This document covers the backend service located in `backend/backend`.

## Overview
- Java 21, Spring Boot 3.5.x
- REST API with layered architecture (controllers → services → repositories)
- JWT authentication (stateless) with RBAC
- MySQL for dev/prod; H2 for tests
- Reporting (CSV/PDF), QR codes, auditing, scheduled tasks

## Prerequisites
- JDK 21+
- MySQL 8.x (local dev)
- Maven Wrapper (mvnw/mvnw.cmd included)

## Configuration
All keys are configurable via environment variables; defaults are defined in `src/main/resources/application.properties`.
- DB_URL (default: `jdbc:mysql://localhost:3306/clims?useSSL=false&serverTimezone=UTC`)
- DB_USERNAME (default: `root`)
- DB_PASSWORD
- JWT_SECRET (Base64-encoded 256-bit secret)
- JWT_EXPIRATION_MS (default: `86400000`)
- CORS_ALLOWED_ORIGINS (default: `http://localhost:3000,http://localhost:4200`)
 - CORS_ALLOWED_METHODS (default: `GET,POST,PUT,PATCH,DELETE,OPTIONS`)
 - CORS_ALLOWED_HEADERS (default: `Authorization,Content-Type`)

Profiles
- `dev`: see `src/main/resources/application-dev.properties` (SQL logging enabled, Swagger UI enabled, default local MySQL db `clims_dev`).
- `prod`: see `src/main/resources/application-prod.properties` (Swagger UI disabled by default, env-driven configs).

Env template
- See `backend/backend/.env.example` for common variables you can set in your environment.

Windows PowerShell example (session-scoped):
```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/clims?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "yourPassword"
$env:JWT_SECRET = "<base64-256-bit-secret>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:3000,http://localhost:4200"
$env:CORS_ALLOWED_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS"
$env:CORS_ALLOWED_HEADERS = "Authorization,Content-Type"
```

## Running locally
From `backend/backend`:
```powershell
# Run API
./mvnw.cmd spring-boot:run

# Run with a profile
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

- Base URL: `http://localhost:8080`
- Health check: `GET /actuator/health`
- API Docs (Swagger UI): `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Seeder creates default admin on first run:
  - username: `admin`
  - password: `Admin@123`

### Authentication
1) Login to get a JWT:
```http
POST /api/auth/login
Content-Type: application/json

{ "username": "admin", "password": "Admin@123" }
```
2) Use the token for subsequent requests:
```
Authorization: Bearer <token>
```

### Swagger UI with JWT
- Open `http://localhost:8080/swagger-ui/index.html`
- Click “Authorize” (lock icon)
- Enter: `Bearer <token>` and press Authorize
- Now secured endpoints will be callable from Swagger UI

### Current User Endpoint
Provides basic information about the authenticated user.

Request
```
GET /api/auth/me
Accept: application/json
Authorization: Bearer <token>
```

Responses
- 200 OK
```
{
  "id": 1,
  "username": "admin",
  "email": "admin@example.com",
  "role": "ADMIN",
  "department": "IT"
}
```
- 401 Unauthorized
```
{ "error": "Unauthorized" }
```

## Build & Test
From `backend/backend`:
```powershell
# Run tests (H2 + test properties)
./mvnw.cmd test

# Build jar
./mvnw.cmd -DskipTests package
```
Jar output is under `backend/backend/target`.

## Database migrations (Flyway)
- Migrations live under `src/main/resources/db/migration`.
- Baseline: `V1__baseline.sql` creates all core tables with FKs, constraints, and indexes.
- Seed: `V2__seed_reference_data.sql` inserts departments, locations, and a default vendor.
- A Java `DataSeeder` creates the default admin user with a BCrypt hash on first run (idempotent).

Notes
- Dev profile defaults to H2 in-memory with MySQL compatibility; prod/dev can point to MySQL 8+.
- JPA uses `ddl-auto=validate`; schema changes should be applied via Flyway.
- See `docs/ERD.md` for the logical ERD and relationships.

## Postman collection
Import `backend/backend/postman_collection.json` and set the variables:
- `baseUrl` (default `http://localhost:8080`)
- `token` (paste the JWT from login)

## Key Endpoints (non-exhaustive)
- `POST /api/auth/login` → obtain JWT
- `GET /api/assets` → list assets
- `POST /api/assets` → create asset (role-restricted)
- `POST /api/maintenance` → schedule maintenance (role-restricted)
- `GET /api/reports/*` → CSV/PDF exports
- `GET /api/assets/{id}/audit` → audit trail for an asset (role-restricted)

Admin user management (ADMIN only)
- `GET /api/users` → paginated list; filters: `role`, `departmentId`, `q`, `page`, `size`, `sort`
- `PATCH /api/users/{id}/role` → body: `{ "role": "ADMIN|MANAGER|IT_STAFF|..." }`
- `PATCH /api/users/{id}/department` → body: `{ "departmentId": 123 }`

## Security & Roles
Configured in `SecurityConfig` with stateless JWT and RBAC. Roles include:
`ADMIN`, `IT_STAFF`, `EMPLOYEE`, `MANAGER`, `TECHNICIAN`, `AUDITOR`, `FINANCE`, `VENDOR`.

## CORS
- Applied globally for `/api/**` with profile-aware defaults.
- Allowed origins, methods, and headers are configured via `CORS_ALLOWED_*` envs (see above).
- Exposes `Content-Disposition`, `X-Report-Limited`, and `X-Report-Limit` headers so browsers can access report filename and truncation info.

## Reporting notes
- CSV responses stream with `Content-Disposition: attachment; filename=<report>_<YYYY-MM-DD>.csv`.
- PDF responses return binary with `Content-Disposition: attachment; filename=<report>_<YYYY-MM-DD>.pdf`.
- For unfiltered requests, large reports are limited (default limit: 5000 rows). When limiting occurs, responses include:
  - `X-Report-Limited: true`
  - `X-Report-Limit: 5000`
- To avoid truncation, provide filters in the request body (e.g., status/date ranges).

## Notes
- Global exception handler standardizes errors (e.g., 404 via NotFoundException)
- Auditing and scheduled tasks (e.g., warranty alerts) are enabled
- Update `CORS_ALLOWED_ORIGINS` to match your frontend dev server
