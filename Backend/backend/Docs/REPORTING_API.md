# Reporting API: Maintenance Workload Trends

Endpoint
- Method: GET
- Path: /api/reports/maintenance/workload
- Auth: JWT required
- Authorization: roles ADMIN or REPORT_VIEWER
- Description: Returns per-day counts of maintenance items by status in the given date range.
 - Discoverability: Available in Swagger UI at /swagger-ui.html (via Springdoc OpenAPI).

Query parameters
- from (ISO date, required): Start date inclusive, e.g., 2025-10-01
- to (ISO date, required): End date inclusive, e.g., 2025-10-09

Constraints and validation
- from and to must be provided.
- to must be on or after from.
- Maximum window size is 366 days (inclusive). Requests exceeding this return 400 Bad Request.
 - The query upper bound is inclusive through 23:59:59.999999999 of the `to` date.

Response
- 200 OK: JSON array of objects sorted by day ascending.
  - day (string, ISO date): The calendar day.
  - status (string): MaintenanceStatus enum value.
  - count (number): Count of records for that day and status.
- 400 Bad Request: Invalid dates or window too large.
- 401 Unauthorized: Missing/invalid token.
- 403 Forbidden: Authenticated but lacking required role.

Example
Request:
  GET /api/reports/maintenance/workload?from=2025-10-01&to=2025-10-09

Successful response body (truncated example):
[
  { "day": "2025-10-01", "status": "REPORTED", "count": 3 },
  { "day": "2025-10-01", "status": "IN_PROGRESS", "count": 1 },
  { "day": "2025-10-02", "status": "RESOLVED", "count": 2 }
]

Notes
- The endpoint uses an efficient JPA projection and database grouping: FUNCTION('date', reportedAt) and status.
- For inclusive upper bound, the controller internally queries up to to 23:59:59.999999999.
- ADMIN role can always access. REPORT_VIEWER can be added to users to allow read-only reporting access.
 - OpenAPI annotations are present on the controller for documentation and client generation.

---

# Admin: ETL Trigger for Maintenance Activity Daily

Endpoint
- Method: POST
- Path: /api/admin/etl/maintenance-activity-daily
- Auth: JWT required
- Authorization: role ADMIN
- Description: Deletes and backfills rows in `maintenance_activity_daily` for the given inclusive date range using aggregated data from `Maintenance`.

Query parameters
- from (ISO date, required): Start date inclusive
- to (ISO date, required): End date inclusive

Constraints and validation
- from and to must be provided; to must be on/after from.
- Maximum window size is 366 days (inclusive). Exceeding returns 400.
- Operation is idempotent for the same window (delete-then-insert semantics).

Response
- 200 OK: JSON body with the run summary
  - from (ISO date)
  - to (ISO date)
  - rowsDeleted (number)
  - rowsInserted (number)
- 400 Bad Request: Invalid dates or window too large.
- 401 Unauthorized / 403 Forbidden: Missing/insufficient privileges.

Example
Request:
  POST /api/admin/etl/maintenance-activity-daily?from=2025-10-01&to=2025-10-09

Successful response body:
{
  "from": "2025-10-01",
  "to": "2025-10-09",
  "rowsDeleted": 0,
  "rowsInserted": 12
}

Metrics
- reporting.etl.maintenance_activity_daily.rows_inserted
- reporting.etl.maintenance_activity_daily.rows_deleted

---

# Admin: ETL Trigger for Asset Status Daily

Endpoint
- Method: POST
- Path: /api/admin/etl/asset-status-daily
- Auth: JWT required
- Authorization: role ADMIN
- Description: Deletes and backfills rows in `asset_status_daily` for the given inclusive date range using aggregated data from `Asset`.

Query parameters
- from (ISO date, required): Start date inclusive
- to (ISO date, required): End date inclusive

Constraints and validation
- from and to must be provided; to must be on/after from.
- Maximum window size is 366 days (inclusive). Exceeding returns 400.
- Operation is idempotent for the same window (delete-then-insert semantics).

Response
- 200 OK: JSON body with the run summary
  - from (ISO date)
  - to (ISO date)
  - rowsDeleted (number)
  - rowsInserted (number)
- 400 Bad Request: Invalid dates or window too large.
- 401 Unauthorized / 403 Forbidden: Missing/insufficient privileges.

Example
Request:
  POST /api/admin/etl/asset-status-daily?from=2025-10-01&to=2025-10-09

Successful response body:
{
  "from": "2025-10-01",
  "to": "2025-10-09",
  "rowsDeleted": 0,
  "rowsInserted": 2
}

Metrics
- reporting.etl.asset_status_daily.rows_inserted
- reporting.etl.asset_status_daily.rows_deleted

Notes
- Asset bucketing is by `purchaseDate` (LocalDate). The ETL uses the inclusive date range as-is.

---

# Admin: ETL Trigger for Audit Action Daily

Endpoint
- Method: POST
- Path: /api/admin/etl/audit-action-daily
- Auth: JWT required
- Authorization: role ADMIN
- Description: Deletes and backfills rows in `audit_action_daily` for the given inclusive date range using aggregated data from `AuditEvent`.

Query parameters
- from (ISO date, required): Start date inclusive (UTC)
- to (ISO date, required): End date inclusive (UTC)

Constraints and validation
- from and to must be provided; to must be on/after from.
- Maximum window size is 366 days (inclusive). Exceeding returns 400.
- Operation is idempotent for the same window (delete-then-insert semantics).

Response
- 200 OK: JSON body with the run summary
  - from (ISO date)
  - to (ISO date)
  - rowsDeleted (number)
  - rowsInserted (number)
- 400 Bad Request: Invalid dates or window too large.
- 401 Unauthorized / 403 Forbidden: Missing/insufficient privileges.

Example
Request:
  POST /api/admin/etl/audit-action-daily?from=2025-10-01&to=2025-10-09

Successful response body:
{
  "from": "2025-10-01",
  "to": "2025-10-09",
  "rowsDeleted": 0,
  "rowsInserted": 3
}

Metrics
- reporting.etl.audit_action_daily.rows_inserted
- reporting.etl.audit_action_daily.rows_deleted

Notes
- Audit bucketing uses UTC calendar days derived from `timestamp` (Instant). The ETL constructs an inclusive window [00:00:00..23:59:59.999999999] in UTC.
