# Phase 3 – Eventing & Notification Architecture (Preliminary Epics)

Goal: Introduce a resilient internal domain event pipeline enabling future notifications, integration hooks, and async side-effect handling without coupling core transaction logic to delivery concerns.

## Guiding Principles
- Transactional integrity first: persist domain intent before publishing.
- Replayable: events stored durably (outbox) until acknowledged by a dispatcher.
- Idempotent consumers: every downstream handler must tolerate duplicates.
- Back-pressure aware: bounded dispatch queues / scheduled draining.
- Observability: metrics + trace IDs on event creation, queue size, failure counts.

## High-Level Architecture
```
[Domain Service]
    | (within TX)
    v
 [Domain Event Entity] --> committed with aggregate
    |
    | (poll / schedule)
    v
 [Outbox Dispatcher] --> publish (in-proc now, pluggable MQ future)
                        |--> Notification Handler (email/slack stub)
                        |--> Audit Propagation (optional enrich)
                        |--> Future: Webhook Fanout
```

## Epics
### 1. Outbox Foundation
- Create `outbox_event` table (id, aggregate_type, aggregate_id, event_type, payload JSON, status, attempt_count, created_at, next_attempt_at, correlation_id, request_id, version).
- Add lightweight `DomainEvent` marker + factory helpers.
- Publish domain events (e.g., AssetCreated, AssetAssigned, MaintenanceScheduled) inside existing service transactions.
- Acceptance Criteria:
  - Event row inserted atomically with aggregate write.
  - Unit test verifying rollback prevents orphan events.
  - Metrics: `outbox.insert.success`, `outbox.insert.failure`.

### 2. Dispatcher Scheduler
- Spring scheduled component scans NEW+RETRY rows (status IN (NEW, RETRY) AND next_attempt_at <= now) with limit & backoff.
- Attempt delivery via in-process handler registry.
- Update status → SENT or RETRY (with exponential backoff and max attempts).
- Acceptance Criteria:
  - Configurable scan interval & batch size.
  - Metrics: `outbox.dispatch.attempt`, `outbox.dispatch.success`, `outbox.dispatch.failure{exception}`.
  - Dead-letter transition after N failures (status=FAILED) with counter `outbox.dispatch.dead`.

### 3. Handler Registry & Contracts
- SPI: `OutboxEventHandler` with supports(eventType) + handle(payload,...).
- Built-in handlers (initial stubs):
  - AuditMirrorHandler (optional extra enrichment / demonstrate multi-handler fanout).
  - NotificationStubHandler (log only).
- Acceptance Criteria:
  - Multiple handlers per event type executed independently.
  - One failing handler does not block others (collect errors, mark partial failures appropriately).

### 4. Idempotency & Deduplication
- Add unique business key (event natural key hash) for events where upstream might retry.
- Handler base class provides idempotent execution using a per-handler processed table or cache.
- Acceptance Criteria:
  - Duplicate insert attempt gracefully no-op (or increments `outbox.duplicate` metric).
  - Load test / unit test simulating rapid duplicate submissions.

### 5. Observability & Tracing Extension
- Propagate `requestId` / `correlationId` into event rows.
- Emit metrics gauges: queue depth (`outbox.queue.depth` by status) refreshed periodically.
- Optional structured log line on each dispatch with IDs.
- Acceptance Criteria:
  - Metrics visible via `/actuator/metrics`.
  - Event row contains correlation fields for future cross-service trace adoption.

### 6. Backoff & Retry Policy Tuning
- Configurable initial delay, multiplier, max delay, max attempts.
- Add `next_attempt_at` field logic.
- Acceptance Criteria:
  - Unit test verifying timeline of attempts given sample policy.
  - Failure path increments attempt count and schedules future retry.

### 7. Operational Tooling
- Admin endpoint (secure) to:
  - Requeue FAILED events (bulk by type/date).
  - Force dispatch of specific ID.
  - Query queue depth snapshot.
- Acceptance Criteria:
  - 403 for non-admin.
  - Audit entry for admin requeue operations.

### 8. Extensibility for External Brokers (Deferred)
- Abstraction layer for dispatcher target (INPROC vs Kafka/Rabbit placeholder).
- Acceptance Criteria:
  - Swappable strategy bean with minimal config changes.
  - Documentation for promoting to external MQ later.

## Data Model Draft (Outbox)
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK | Auto increment |
| aggregate_type | VARCHAR | e.g. `Asset` |
| aggregate_id | VARCHAR | Stringified ID |
| event_type | VARCHAR | Logical event name |
| payload | JSON/Text | Serialized domain payload |
| status | ENUM | NEW / RETRY / SENT / FAILED |
| attempt_count | INT | Increment per dispatch try |
| next_attempt_at | TIMESTAMP | Retry scheduling |
| correlation_id | VARCHAR | From request context |
| request_id | VARCHAR | From request context |
| version | INT | Payload versioning |
| created_at | TIMESTAMP | Insert time |
| updated_at | TIMESTAMP | Mutation time |

Indexes:
- (status, next_attempt_at)
- (event_type)
- (aggregate_type, aggregate_id)

## Metrics Summary (Planned)
| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `outbox.insert.success` | counter | — | Rows inserted |
| `outbox.insert.failure` | counter | exception | Insert failures |
| `outbox.dispatch.attempt` | counter | — | Each dispatch loop try |
| `outbox.dispatch.success` | counter | — | Successful handler executions |
| `outbox.dispatch.failure` | counter | handler,exception | Failed handler execution |
| `outbox.dispatch.dead` | counter | — | Events marked FAILED permanently |
| `outbox.queue.depth` | gauge | status | Row counts per status |
| `outbox.duplicate` | counter | — | Suppressed duplicate events |

## Acceptance Done Definition (Phase 3)
- All epics 1–5 implemented and tested.
- Retry/backoff policy externally configurable and documented.
- Basic admin requeue endpoint shipped (epic 7 partial OK; external MQ deferred).
- Documentation updated (README + architecture diagram snippet).

## Risks & Mitigations
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Large payload bloat | Storage + dispatch slowdown | Keep payload minimal (aggregate id + diff) |
| Handler side-effects non-idempotent | Duplicate external actions | Enforce idempotent contract + processed table |
| Dispatcher starvation under high load | Event lag | Batch size tuning + metrics alerting |
| Transactional latency | Commit slowed by serialization | Lightweight payload assembly only (no remote calls) |

## Next Steps After Phase 3
- External broker integration (Kafka/Rabbit).
- User-facing notification delivery (email / WebSocket push).
- SLA instrumentation & lag histograms.

---
This draft may evolve; refine before implementation sprint kicks off.
