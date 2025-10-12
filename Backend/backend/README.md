# Backend

## Swagger UI

When running the backend locally, the OpenAPI/Swagger UI is available at:

- http://localhost:8080/swagger-ui.html

Security: These reporting endpoints require a valid JWT with roles `ADMIN` or `REPORT_VIEWER`.

### OpenAPI Artifacts in CI

On every push/PR to `main` that touches `Backend/**`, CI builds the app and uploads the OpenAPI artifacts:

- `openapi-spec/openapi.json`
- `openapi-spec/openapi.yaml`

Release tags (`v*`) also attach these specs to the GitHub Release.

### Standardized Error Responses

The API exposes a standardized error model used across endpoints. See:

- `Docs/API_ERRORS.md` (contract and examples)
- Swagger responses now include example error payloads for common codes (400/401/403/404/409/500).

## Reporting endpoints

- GET /api/reports/maintenance/workload?from=YYYY-MM-DD&to=YYYY-MM-DD
- GET /api/reports/assets/daily-status?from=YYYY-MM-DD&to=YYYY-MM-DD
- GET /api/reports/audit/daily-actions?from=YYYY-MM-DD&to=YYYY-MM-DD

Each returns a JSON array of daily rows within the inclusive date window (max 366 days).

## Running locally

You can run the backend with the default local profile, or optionally enable an insecure dev profile that permits all requests.

### Default (secured)

```
# Maven
./mvnw spring-boot:run

# JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### Dev-only insecure mode (permit all)

For quick iteration, you can opt into an insecure profile that disables authentication entirely. This is intended only for local development.

Safety guardrails:
- Binds HTTP and Actuator to 127.0.0.1 only
- Actuator exposes only health,info

To run with insecure mode:

```
# Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,insecure

# JAR
java -jar target/backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local,insecure
```

Do NOT enable the insecure profile in CI/CD or any shared/staging/prod environment.
