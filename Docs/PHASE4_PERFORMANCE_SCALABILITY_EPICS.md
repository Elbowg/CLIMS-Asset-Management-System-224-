# Phase 4 – Performance & Scalability Strategy (Preliminary Epics)

Goal: Establish measurable performance baselines, remove early bottlenecks, and introduce infrastructure & code patterns that allow linear capacity scaling under growing asset + audit workloads.

## Performance Objectives (Draft KPIs)
| Metric | Target (Initial) | Notes |
|--------|------------------|-------|
| P95 Asset List (paginated) | < 150 ms | Warm JVM, 10k assets |
| P95 Auth (login) | < 120 ms | Excludes cold DB start |
| P95 Audit Write | < 40 ms | Single row insert |
| Throughput (asset list) | 300 RPS | Horizontal scaling w/ 3 instances |
| DB CPU @ steady 5 req/s mix | < 40% | Avoid early saturation |
| Memory footprint | < 512MB heap | Default sizing |

## Guiding Principles
- Optimize only backed by measurement (no speculative micro-optimizations).
- Prefer pagination + projection over large object graphs.
- Control N+1 via fetch strategies and DTO mapping.
- Maintain determinism: performance enhancements must not change business semantics.

## Epics
### 1. Baseline Profiling & Benchmark Harness
- Introduce JMH (micro) + Gatling or k6 (macro) scaffolding.
- Scenarios: login, asset list (page 0, varying sizes), asset assignment, audit event burst.
- Store baseline results under `Docs/perf/baseline-*`.
- Acceptance Criteria:
  - `perf` Maven module builds separately (skipped by default profile).
  - Document how to run micro vs macro benchmarks.

### 2. Query & Pagination Hardening
- Enforce pagination on all collection endpoints (default + max page size policy).
- Replace `SELECT *` style entity loads with projection for list endpoints.
- Acceptance Criteria:
  - Asset listing uses projection (id, name, status, owner summary) only.
  - Tests verifying page size capping and deterministic ordering.

### 3. N+1 Detection & Prevention
- Enable Hibernate statistics (dev profile) and add simple detector logs (>X queries per request).
- Add test that triggers asset list with related owners to assert bounded query count.
- Acceptance Criteria:
  - Metrics: `orm.query.count` gauge per endpoint sample (optional custom binder).
  - Documentation for identifying N+1 in logs.

### 4. Caching Layer (Selective)
- Introduce Spring Cache abstraction (Caffeine) for metadata lookups (e.g., role list, status taxonomy) where applicable.
- Guard with TTL + max size; include eviction metrics.
- Acceptance Criteria:
  - Cache config in `application-cache.yml` (imported conditionally).
  - Metrics: `cache.gets`, `cache.puts`, `cache.evictions` visible in actuator.

### 5. Audit Insert Optimization
- Batch or tune insert path (consider disabling flush per row in loops; ensure index selectivity adequate).
- Evaluate secondary index utility with EXPLAIN (document rationale to keep/drop).
- Acceptance Criteria:
  - Document forward-only audit ingestion rate test (e.g., 500 events/s for short burst) and results.

### 6. Connection Pool & Thread Tuning
- Expose Hikari pool metrics; set initial sizing guidelines.
- Align web thread pool (Tomcat/Jetty) with target concurrency.
- Acceptance Criteria:
  - Config table documenting rationale (pool size = (core * 2) + spares heuristic or measured).

### 7. Load & Stress Testing Pipeline
- Add scripted load test (k6 or Gatling) with environment variable overrides.
- CI optional job to run smoke performance scenario (short, <2 min) to catch regressions.
- Acceptance Criteria:
  - Threshold assertions (e.g., fail build if P95 > baseline * 1.5x).

### 8. Metrics & Alerting Enhancements
- Introduce latency histograms (`@Timed` or micrometer timers) for critical endpoints.
- Add custom timer for audit retention job.
- Acceptance Criteria:
  - `/actuator/metrics/http.server.requests` shows expected tag cardinality (method,status,uri).
  - Docs include recommended Prometheus alert rules skeleton.

### 9. Resilience & Degradation Patterns
- Add timeouts + fallback (where sensible) for any future external calls (placeholder).
- Circuit breaker library evaluation (Resilience4j) deferred until external integrations appear.
- Acceptance Criteria:
  - Placeholder wiring with disabled profile flag; docs clarify activation path.

### 10. Memory & GC Observability
- Add jvm.* metric interpretation guide.
- Optionally enable Async Profiler / JFR instructions in docs.
- Acceptance Criteria:
  - Docs: how to capture a 30s CPU + alloc profile in staging.

## Risk Matrix
| Risk | Impact | Mitigation |
|------|--------|-----------|
| Premature caching | Stale data / invalidation bugs | Start with read-only metadata only |
| Over-sized pool | Resource waste | Profile & scale incrementally |
| Histogram cardinality explosion | Metrics backend pressure | Limit `uri` patterns (use templated endpoints) |
| Test flakiness under load | CI noise | Keep smoke scenario minimal; isolate full load off CI |

## Tooling Stack (Proposed)
| Layer | Tool |
|-------|------|
| Microbenchmarks | JMH |
| Load testing | Gatling or k6 (HTTP) |
| Profiling (JVM) | JFR + Async Profiler (manual) |
| Metrics backend | Prometheus (future) |

## Deliverables Definition (Phase 4 Complete)
- Benchmark harness committed with reproducible baseline docs.
- Mandatory pagination + projections in high-volume endpoints.
- N+1 guardrails + at least one prevention test.
- Optional caching (documented) with metrics visible.
- Timer metrics + histogram config in place.
- Performance README section summarizing running & interpreting outputs.

## Stretch Goals
- Distributed tracing latency breakdown once OTel introduced.
- Adaptive concurrency experimentation.

---
This plan will be refined before implementation to adjust targets after initial baseline capture.
