# Phase 5 – Advanced Analytics & Reporting (Preliminary Epics)

Goal: Provide stakeholders with actionable insights (utilization, lifecycle velocity, maintenance trends) while keeping OLTP load isolated from analytical queries.

## Analytical Themes
- Asset Utilization & Turnover
- Maintenance Scheduling Efficiency
- User / Department Allocation Patterns
- Audit Trend Insight (security anomalies, access spikes)

## Guiding Principles
- Do not burden the primary OLTP schema with heavy aggregations.
- Prefer incremental materialization over ad-hoc full scans.
- Enable export interfaces (CSV/JSON) with pagination and rate guards.
- Preserve PII / sensitive data boundaries (role-gated reporting endpoints).

## Architecture Options (to evaluate)
| Option | Description | Pros | Cons |
|--------|-------------|------|------|
| On-demand SQL + projections | Direct queries with tuned indexes | Simple, low infra | Risk of OLTP contention |
| Materialized summary tables | Nightly/rolling aggregation jobs | Fast queries | ETL complexity |
| Event-driven analytic store | Stream outbox -> analytics DB | Near real-time | Infra overhead |
| External BI Export | Provide cleaned staging export | Decouples compute | Latency + governance |

Initial recommendation: Start with hybrid (summary tables refreshed on schedule + limited on-demand detail queries) then evolve to event-driven if required.

## Epics
### 1. Reporting Security & Access Model
- Define `REPORT_VIEWER` role (or reuse ADMIN subset).
- Central authorization checks for report endpoints.
- Acceptance Criteria:
  - 403 on unauthorized access verified by tests.
  - Security documentation updated with role matrix.

### 2. Summary Schema Introduction
- Create tables: `asset_status_daily`, `maintenance_activity_daily`, `audit_action_daily`.
- Fields include date, dimension keys, counts, aggregates (e.g., avg age, new vs retired counts).
- Current status:
  - Implemented: `maintenance_activity_daily` with columns `bucket_date`, `maintenance_status`, `activity_count` (Flyway `V21__create_summary_tables.sql`).
  - Column names intentionally avoid reserved keywords for cross-DB compatibility (H2/MySQL).
  - JPA entity `MaintenanceActivityDaily` with embedded id `MaintenanceActivityDailyId` created.
  - Repository `MaintenanceActivityDailyRepository` with date-range finder and bulk delete.
- Acceptance Criteria:
  - Flyway migration adds tables + necessary composite indexes (date + dimension).
  - Data model documented with refresh semantics.

### 3. ETL / Aggregation Job
- Scheduled job (daily + manual trigger) populates / upserts summary rows (truncate + rebuild or incremental diff strategy).
- Supports backfill for past N days.
- Current status:
  - Implemented: `ReportingEtlService.backfillMaintenanceActivityDaily(from,to)`
    - Validates inclusive window (≤ 366 days), queries source via `MaintenanceRepository.findDailyStatusCounts(...)`.
    - Idempotent: deletes existing rows in range, then inserts aggregated rows.
    - Micrometer counters: `reporting.etl.maintenance_activity_daily.rows_inserted`, `reporting.etl.maintenance_activity_daily.rows_deleted`.
  - Admin trigger endpoint: POST `/api/admin/etl/maintenance-activity-daily?from=YYYY-MM-DD&to=YYYY-MM-DD` (role: ADMIN) with 400 on invalid window.
  - Tests: Integration test covers backfill and idempotency.
- Next:
  - Add scheduler for nightly backfill and configurable retention purge for summary tables.
  - Extend ETL to `asset_status_daily` and `audit_action_daily`.
- Acceptance Criteria:
  - Configurable retention (e.g., 365d) for summary tables.
  - Metrics: success/failure/run-duration and rows processed tracked; alerts can be added post-MVP.

### 4. Reporting API Endpoints
- Endpoints (secured):
  - `/api/reports/assets/status-trend?from=...&to=...`
  - `/api/reports/maintenance/workload?from=...&to=...`
  - `/api/reports/audit/actions?from=...&to=...&action=LOGIN`
- Pagination + date validation + max window enforced.
- Acceptance Criteria:
  - OpenAPI docs for parameters + examples.
  - Tests verifying date window enforcement and role gating.
  - Status: `GET /api/reports/maintenance/workload` shipped with validation (≤ 366 days), inclusive end-of-day, and roles ADMIN/REPORT_VIEWER; OpenAPI annotations and tests are in place.

### 5. Export Functionality
- CSV & JSON streaming exports with content-disposition headers.
- Throttle via simple in-memory token bucket or rate limiter (future Resilience4j if adopted).
- Acceptance Criteria:
  - Large export does not OOM (streamed response).
  - Metrics: `report.export.request`, `report.export.bytes`.

### 6. Query Optimization & Indexing
- Add targeted covering indexes for high-frequency filters (date range + action / status).
- Validate via EXPLAIN; document plans before/after.
- Acceptance Criteria:
  - Query plan docs stored in `Docs/analytics/query-plans`.

### 7. Data Quality & Drift Detection
  - Implement scheduled drift checks comparing source vs summary tables and emit metrics; alert if thresholds breached.
  - Status: Implemented for maintenance_activity_daily (see OBSERVABILITY.md for metrics `report.quality.mismatch.days`, `report.quality.mismatch.rows`, `report.quality.mismatch.abs_delta`, and `report.quality.check.*`).
  - Next: Extend to asset_status_daily and audit_action_daily after their ETLs are added.
  - Acceptance Criteria:
    - Nightly check runs on schedule and records metrics.
    - Alerts configured for persistent mismatches (> 0 for 2 consecutive runs) and large absolute deltas.

### 8. Anomaly Detection (Stretch)
See `Docs/OBSERVABILITY.md` for:
- reporting drift metrics (report.quality.*)
- MDC fields (requestId, correlationId, user)
- suggested dashboards and alerts
- Simple statistical baseline (e.g., 3σ) for spikes in audit LOGIN failures or asset retirements.
- Optional: emit event to outbox for notification system (post-Phase 3 completion).
- Acceptance Criteria:
  - Prototype service with pluggable detection strategy.

### 9. Documentation & Operational Playbook
- Add README section: architecture diagrams, role matrix, refresh schedule, latency expectations.
- Provide sample Prometheus alert rules (lag, ETL failures, data mismatch).
- Acceptance Criteria:
  - All metrics enumerated in docs.

## Metrics Summary (Planned)
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `report.etl.run.success` | counter | — | Successful ETL executions |
| `report.etl.run.failure` | counter | exception | Failed ETL executions |
| `report.etl.rows.processed` | counter | table | Rows processed per summary table |
| `reporting.etl.maintenance_activity_daily.rows_inserted` | counter | — | Rows inserted by maintenance ETL |
| `reporting.etl.maintenance_activity_daily.rows_deleted` | counter | — | Rows deleted for idempotent refresh |
| `report.export.request` | counter | format | Export requests initiated |
| `report.export.bytes` | counter | format | Bytes streamed |
| `report.quality.mismatch.days` | gauge | summary | Days with at least one mismatch in window |
| `report.quality.mismatch.rows` | gauge | summary | Count of mismatched (day,dimension) rows |
| `report.quality.mismatch.abs_delta` | gauge | summary | Sum of absolute deltas across mismatches |
| `report.quality.check.runs` | counter | summary | Number of drift check executions |
| `report.quality.check.duration` | timer | summary | Duration of each drift check run |

## Acceptance Done Definition (Phase 5 Complete)
- Core summary tables + ETL job.
- Secured reporting endpoints with OpenAPI.
- Export streaming working for at least one report.
- Metrics + documentation shipped.

Current progress toward Done:
- Summary schema created for maintenance activity daily; entity/repo done.
- ETL service + admin trigger endpoint implemented and tested.
- Maintenance workload endpoint (on-demand) implemented with tests and docs.
- Drift checker implemented with unit tests and observability docs.
Remaining for completion:
- Scheduler + retention, extend ETL to asset/audit summaries, add CSV export endpoint, finalize docs and alerting examples.
## Risks & Mitigations
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Long ETL runtime | Stale summaries | Incremental aggregation + indexes |
| Role sprawl | Complexity | Consolidate to minimal reporting roles |
| Large exports degrade DB | Production load | Pagination + rate limiting |
| Data drift unnoticed | Incorrect decisions | Automated mismatch metrics + alerts |

## Future Evolution
- Event-driven ingestion to analytics store (Kafka → OLAP engine).
- Columnar warehouse integration (e.g., ClickHouse / DuckDB / BigQuery) for heavy queries.
- Pre-computed dashboards or embedding with BI tools.

---
This plan sets the foundation; refine KPIs after initial baselining in Phase 4.
