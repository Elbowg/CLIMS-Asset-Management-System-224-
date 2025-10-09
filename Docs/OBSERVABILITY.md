# Observability: Metrics and Structured Logging

This document describes new metrics and logging context added for reporting data quality and request tracing.

## Metrics: report.quality.*

The scheduled checker `ReportingDriftChecker` compares source-of-truth maintenance aggregations with the summary table `maintenance_activity_daily` over a rolling window (default 14 days) and emits Micrometer metrics.

- report.quality.mismatch.days (gauge)
  - Description: Number of days in the window with at least one row mismatch
  - Tags: summary=maintenance_activity_daily
- report.quality.mismatch.rows (gauge)
  - Description: Number of (day,status) rows that differ between source and summary
  - Tags: summary=maintenance_activity_daily
- report.quality.mismatch.abs_delta (gauge)
  - Description: Sum of absolute differences across all mismatched rows
  - Tags: summary=maintenance_activity_daily
- report.quality.check.runs (counter)
  - Description: Number of drift check executions
  - Tags: summary=maintenance_activity_daily
- report.quality.check.duration (timer)
  - Description: Duration of each drift check run
  - Tags: summary=maintenance_activity_daily

Scheduling:
- Cron: `0 10 2 * * *` (02:10 daily)
- Window size: `reporting.drift.window-days` (default 14)

Suggested alerts:
- mismatch.days > 0 for 2 consecutive runs
- mismatch.abs_delta > 1 for 2 consecutive runs

Dashboards:
- Panel 1: mismatch.days (stat)
- Panel 2: mismatch.rows (line)
- Panel 3: mismatch.abs_delta (line)
- Panel 4: check.duration (histogram/heatmap)

Example Prometheus-style queries (adapt as needed):
- Days with mismatches (rolling):
  - max by (summary) (report_quality_mismatch_days)
- Row mismatches time series:
  - sum by (summary) (rate(report_quality_mismatch_rows[5m]))
- Absolute delta:
  - max by (summary) (report_quality_mismatch_abs_delta)
- Check duration p95 (histogram):
  - histogram_quantile(0.95, sum by (le, summary) (rate(report_quality_check_duration_seconds_bucket[5m])))

## Structured Logging (MDC)

The web layer enriches logs with these MDC keys:
- requestId: unique id per request (also returned in response headers)
- correlationId: forwarded/propagated id (also echoed back)
- user: authenticated username (when available)

Implementation:
- `RequestIdFilter` sets `requestId` and `correlationId` in MDC and HTTP headers
- `MdcUserFilter` adds `user` to MDC from the authenticated principal

Recommended log pattern (Logback):
```
%d{ISO8601} %-5level [%X{requestId} %X{correlationId} %X{user}] %logger - %msg%n
```

If you want JSON logs, consider Logstash JSON encoder and include MDC automatically:
```
<encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
  <providers>
    <timestamp>
      <timeZone>UTC</timeZone>
    </timestamp>
    <logLevel/>
    <loggerName/>
    <threadName/>
    <mdc/>
    <message/>
    <stackTrace/>
  </providers>
</encoder>
```

Sample logback-spring.xml snippet to set the pattern above and ensure MDC fields are present:
```
<configuration>
  <property name="PATTERN" value="%d{ISO8601} %-5level [%X{requestId} %X{correlationId} %X{user}] %logger - %msg%n"/>
  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>${PATTERN}</pattern>
      <charset>UTF-8</charset>
    </encoder>
  </appender>
  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
  </root>
</configuration>
```

Notes:
- Filters clean up MDC after each request
- Use the MDC fields in dashboards to slice logs per user, request, or correlation

## Next steps
- Add similar drift metrics for asset_status_daily and audit_action_daily when those ETLs are implemented.
- Wire alerts in your monitoring tool (e.g., Prometheus/Alertmanager, App Insights, etc.).

Alert examples (pseudo):
- If days mismatch persists:
  - condition: report_quality_mismatch_days > 0 for 2 runs
  - severity: warning
  - summary: "Reporting drift detected in maintenance_activity_daily"
- If absolute delta spikes:
  - condition: report_quality_mismatch_abs_delta > 10 for 1 run
  - severity: critical
  - summary: "Large reporting drift (abs delta > 10)"

## Runbook: Responding to Drift Alerts

When an alert fires for report data drift:

1) Confirm the signal
- Inspect `report.quality.mismatch.days`, `report.quality.mismatch.rows`, and `report.quality.mismatch.abs_delta`.
- Check `report.quality.check.runs` and the application logs: `ReportingDriftChecker` logs the exact window (from..to) and counts.

2) Identify scope
- Note the affected summary (tag summary=maintenance_activity_daily for now).
- Determine whether the mismatch is limited to a single day or many days.

3) Remediate by backfilling
- Use the admin endpoint to backfill the exact window (inclusive):
  - `POST /api/admin/etl/maintenance-activity-daily?from=YYYY-MM-DD&to=YYYY-MM-DD`
- The ETL is idempotent (delete-then-insert). Choose a narrow window first (e.g., one day) to validate results.

4) Verify resolution
- After ETL, wait for the next scheduled drift check or monitor gauges returning to 0.
- Optionally, temporarily expand the drift window property to recheck a broader period (if operationally acceptable).

5) If drift persists
- Investigate source queries vs summary schema (time zone boundaries, status enumeration changes, late-arriving data).
- Validate Flyway migrations applied and no schema drift occurred.
- Consider adding a one-off ETL backfill for an extended window.

6) Follow-up
- Capture a short post-incident note: root cause, impacted days, and whether alert thresholds need tuning.
