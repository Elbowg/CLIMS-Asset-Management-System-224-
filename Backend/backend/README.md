# Backend

## Swagger UI

When running the backend locally, the OpenAPI/Swagger UI is available at:

- http://localhost:8080/swagger-ui.html

Security: These reporting endpoints require a valid JWT with roles `ADMIN` or `REPORT_VIEWER`.

## Reporting endpoints

- GET /api/reports/maintenance/workload?from=YYYY-MM-DD&to=YYYY-MM-DD
- GET /api/reports/assets/daily-status?from=YYYY-MM-DD&to=YYYY-MM-DD
- GET /api/reports/audit/daily-actions?from=YYYY-MM-DD&to=YYYY-MM-DD

Each returns a JSON array of daily rows within the inclusive date window (max 366 days).
