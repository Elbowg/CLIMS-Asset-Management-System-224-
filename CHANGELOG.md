# Changelog

All notable changes to this project will be documented in this file.

The format loosely follows Keep a Changelog (https://keepachangelog.com/) and semantic-style categorization, but version numbers are aligned with backend application snapshot progression. Future tagged releases can refine to semantic versions.

## [Unreleased]
- Phase 2 (in progress):
  - Audit retention job (purges old events, configurable retention + metric `audit.retention.purged`).
  - Open Session in View disabled (`spring.jpa.open-in-view=false`) to enforce explicit transactional boundaries.
  - Audit index optimization (descending composite indexes for principal+timestamp, action+timestamp) via migration V17.
  - Expanded security & audit metrics (login/refresh/logout issuance, blacklist additions, rotation counters, audit write success/failure).
 - Phase 3 (initial slice):
   - Outbox foundation: migration V18 creating `outbox_event` table with status, attempts, scheduling fields & indexes.
   - `OutboxEvent` entity + repository + service for transactional event recording (currently used on asset create path).
   - `OutboxDispatcher` scheduled poller with exponential backoff, status transitions (NEW → SENT / RETRY / FAILED), and metrics (`outbox.dispatch.*`, `outbox.insert.*`, `outbox.queue.depth`, `outbox.dispatch.batch.time`).
   - Micrometer instrumentation (counters, gauge, timer) and basic dispatcher unit test.

## [Phase 1 Baseline] - 2025-10-08
### Added
- Rotating JWT secret support with multi-secret validation window (primary used for signing, all secrets for verification).
- Domain metrics binder: asset counts by status (`domain.assets.count`), maintenance total (`domain.maintenance.total`), blacklist size gauge (`auth.blacklist.size`).
- Audit logging infrastructure (`audit_event` table) capturing: principal, action, timestamp, IP, optional requestId and details.
- Standardized error response schema including `requestId` and `correlationId` (traceability across logs, audits, clients).
- OpenAPI documentation for security scheme, error model, and primary auth flows.
- Comprehensive tests (≈40) covering JWT rotation, error schema propagation, audit repository behaviors, security integration, and negative scenarios.

### Database
- Migration V16: `create audit event table` introducing `audit_event` with supporting ascending indexes (superseded by V17 in Phase 2).

### Security / Auth
- Refresh token rotation and revocation (blacklist abstraction with pluggable in-memory / Redis implementations).
- Access tokens embed role authorities; refresh tokens type separated via `typ` claim and validated.

### Observability
- Request-scoped identifiers standardized in responses and audit entries to enable correlation.
- Initial metric coverage for domain + auth success paths.

### Testing & Stability
- Full green suite post-Phase 1 ensuring regression safety for subsequent phases.

## Migration Reference (Phase 1)
| Version | Purpose |
|---------|---------|
| V0-V15  | Core schema, seed users/roles, incremental adjustments (see migration files) |
| V16     | Create `audit_event` table + initial indexes |

> NOTE: V17 (Phase 2) replaces the original audit indexes with descending composites to improve recent-event query performance.

## Planned (Upcoming)
- Tracing documentation (requestId/correlationId + audit interplay).
- Performance & scalability (profiling, caching, pagination strategy) in later phases.
- Eventing/outbox foundation (Phase 3) behind feature flag.

---
For any security-impacting change, also update `SECURITY.md` with operational guidance as needed.
