package com.clims.backend.service.reporting;

import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.model.summary.MaintenanceActivityDaily;
import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.repository.projection.MaintenanceDailyStatusCount;
import com.clims.backend.repository.summary.MaintenanceActivityDailyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodic data-quality drift check comparing source-of-truth Maintenance aggregation
 * to summary table maintenance_activity_daily. Emits Micrometer gauges and counters
 * under the prefix report.quality.*
 */
@Component
public class ReportingDriftChecker {
    private static final Logger log = LoggerFactory.getLogger(ReportingDriftChecker.class);

    private final MaintenanceRepository maintenanceRepository;
    private final MaintenanceActivityDailyRepository summaryRepository;
    private final MeterRegistry meterRegistry;

    private final int windowDays;

    // Gauges (updated per run)
    private final AtomicInteger mismatchDays = new AtomicInteger(0);
    private final AtomicInteger mismatchRows = new AtomicInteger(0);
    private final AtomicLong absDelta = new AtomicLong(0);

    public ReportingDriftChecker(MaintenanceRepository maintenanceRepository,
                                 MaintenanceActivityDailyRepository summaryRepository,
                                 MeterRegistry meterRegistry,
                                 @Value("${reporting.drift.window-days:14}") int windowDays) {
        this.maintenanceRepository = maintenanceRepository;
        this.summaryRepository = summaryRepository;
        this.meterRegistry = meterRegistry;
        this.windowDays = Math.max(1, windowDays);

        // Register gauges once
        meterRegistry.gauge("report.quality.mismatch.days", Tags.of("summary", "maintenance_activity_daily"), mismatchDays);
        meterRegistry.gauge("report.quality.mismatch.rows", Tags.of("summary", "maintenance_activity_daily"), mismatchRows);
        meterRegistry.gauge("report.quality.mismatch.abs_delta", Tags.of("summary", "maintenance_activity_daily"), absDelta);
    }

    /**
     * Default scheduled run once per day at 02:10.
     */
    @Scheduled(cron = "0 10 2 * * *")
    public void scheduledCheck() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(windowDays - 1L);
        runCheckForWindow(from, to);
    }

    /**
     * Public method to run a drift check for a given window. Useful for tests.
     */
    public void runCheckForWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            log.warn("Invalid window for drift check: from={} to={} - skipping", from, to);
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        int localMismatchDays = 0;
        int localMismatchRows = 0;
        long localAbsDelta = 0L;

        try {
            // Source-of-truth aggregation
            LocalDateTime fromTs = from.atStartOfDay();
            LocalDateTime toTs = to.atTime(LocalTime.MAX);
            List<MaintenanceDailyStatusCount> sourceAgg = maintenanceRepository.findDailyStatusCounts(fromTs, toTs);
            Map<LocalDate, Map<MaintenanceStatus, Long>> sourceMap = toMap(sourceAgg);

            // Summary snapshot
            List<MaintenanceActivityDaily> summaries = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(from, to);
            Map<LocalDate, Map<MaintenanceStatus, Long>> summaryMap = new HashMap<>();
            for (MaintenanceActivityDaily row : summaries) {
                LocalDate day = row.getId().getBucketDate();
                MaintenanceStatus status = row.getId().getMaintenanceStatus();
                long count = row.getActivityCount();
                summaryMap.computeIfAbsent(day, d -> new EnumMap<>(MaintenanceStatus.class)).put(status, count);
            }

            // Compare over union of days and statuses
            Set<LocalDate> allDays = new TreeSet<>();
            allDays.addAll(sourceMap.keySet());
            allDays.addAll(summaryMap.keySet());

            for (LocalDate day : allDays) {
                Set<MaintenanceStatus> statuses = new HashSet<>();
                statuses.addAll(Optional.ofNullable(sourceMap.get(day)).map(Map::keySet).orElseGet(Collections::emptySet));
                statuses.addAll(Optional.ofNullable(summaryMap.get(day)).map(Map::keySet).orElseGet(Collections::emptySet));

                boolean dayHasMismatch = false;
                for (MaintenanceStatus st : statuses) {
                    long src = Optional.ofNullable(sourceMap.get(day)).map(m -> m.getOrDefault(st, 0L)).orElse(0L);
                    long sum = Optional.ofNullable(summaryMap.get(day)).map(m -> m.getOrDefault(st, 0L)).orElse(0L);
                    if (src != sum) {
                        dayHasMismatch = true;
                        localMismatchRows++;
                        localAbsDelta += Math.abs(src - sum);
                        log.debug("Drift detected day={} status={} source={} summary={}", day, st, src, sum);
                    }
                }
                if (dayHasMismatch) {
                    localMismatchDays++;
                }
            }

            // Update gauges
            mismatchDays.set(localMismatchDays);
            mismatchRows.set(localMismatchRows);
            absDelta.set(localAbsDelta);

            // Counters
            meterRegistry.counter("report.quality.check.runs", Tags.of("summary", "maintenance_activity_daily")).increment();
            log.info("Drift check window {}..{} daysWithMismatch={} rowMismatches={} absDelta={}", from, to, localMismatchDays, localMismatchRows, localAbsDelta);
        } finally {
            sample.stop(meterRegistry.timer("report.quality.check.duration", Tags.of("summary", "maintenance_activity_daily")));
        }
    }

    private Map<LocalDate, Map<MaintenanceStatus, Long>> toMap(List<MaintenanceDailyStatusCount> rows) {
        Map<LocalDate, Map<MaintenanceStatus, Long>> map = new TreeMap<>();
        for (MaintenanceDailyStatusCount r : rows) {
            map.computeIfAbsent(r.getDay(), d -> new EnumMap<>(MaintenanceStatus.class))
                    .put(r.getStatus(), r.getCount());
        }
        return map;
    }
}
