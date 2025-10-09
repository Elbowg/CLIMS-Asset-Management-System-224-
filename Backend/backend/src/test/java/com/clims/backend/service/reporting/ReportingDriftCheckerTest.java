package com.clims.backend.service.reporting;

import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.model.summary.MaintenanceActivityDaily;
import com.clims.backend.model.summary.MaintenanceActivityDailyId;
import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.repository.projection.MaintenanceDailyStatusCount;
import com.clims.backend.repository.summary.MaintenanceActivityDailyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ReportingDriftCheckerTest {

    @Test
    void updatesMetricsWhenDriftDetected() {
        MaintenanceRepository maintenanceRepository = Mockito.mock(MaintenanceRepository.class);
        MaintenanceActivityDailyRepository summaryRepository = Mockito.mock(MaintenanceActivityDailyRepository.class);
        MeterRegistry registry = new SimpleMeterRegistry();

        // Window: single day
        LocalDate day = LocalDate.now().minusDays(1);
        LocalDateTime fromTs = day.atStartOfDay();
        LocalDateTime toTs = day.atTime(LocalTime.MAX);

    // Source-of-truth: 5 REPORTED
        when(maintenanceRepository.findDailyStatusCounts(eq(fromTs), eq(toTs)))
        .thenReturn(List.of(stubRow(day, MaintenanceStatus.REPORTED, 5)));

    // Summary: 3 REPORTED
    MaintenanceActivityDailyId id = new MaintenanceActivityDailyId(day, MaintenanceStatus.REPORTED);
        when(summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(eq(day), eq(day)))
                .thenReturn(List.of(new MaintenanceActivityDaily(id, 3)));

        ReportingDriftChecker checker = new ReportingDriftChecker(maintenanceRepository, summaryRepository, registry, 7);

        checker.runCheckForWindow(day, day);

        double daysGauge = registry.get("report.quality.mismatch.days").gauge().value();
        double rowsGauge = registry.get("report.quality.mismatch.rows").gauge().value();
        double absDeltaGauge = registry.get("report.quality.mismatch.abs_delta").gauge().value();

        assertThat(daysGauge).isEqualTo(1.0);
        assertThat(rowsGauge).isEqualTo(1.0);
        assertThat(absDeltaGauge).isEqualTo(2.0);

        // Also expect run counter incremented
        double runs = registry.get("report.quality.check.runs").counter().count();
        assertThat(runs).isEqualTo(1.0);
    }

    @Test
    void noDriftLeavesGaugesZero() {
        MaintenanceRepository maintenanceRepository = Mockito.mock(MaintenanceRepository.class);
        MaintenanceActivityDailyRepository summaryRepository = Mockito.mock(MaintenanceActivityDailyRepository.class);
        MeterRegistry registry = new SimpleMeterRegistry();

        LocalDate day = LocalDate.now().minusDays(2);
        LocalDateTime fromTs = day.atStartOfDay();
        LocalDateTime toTs = day.atTime(LocalTime.MAX);

        // Source and summary both show 4 REPORTED
        when(maintenanceRepository.findDailyStatusCounts(eq(fromTs), eq(toTs)))
                .thenReturn(List.of(stubRow(day, MaintenanceStatus.REPORTED, 4)));

        MaintenanceActivityDailyId id = new MaintenanceActivityDailyId(day, MaintenanceStatus.REPORTED);
        when(summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(eq(day), eq(day)))
                .thenReturn(List.of(new MaintenanceActivityDaily(id, 4)));

        ReportingDriftChecker checker = new ReportingDriftChecker(maintenanceRepository, summaryRepository, registry, 7);
        checker.runCheckForWindow(day, day);

        double daysGauge = registry.get("report.quality.mismatch.days").gauge().value();
        double rowsGauge = registry.get("report.quality.mismatch.rows").gauge().value();
        double absDeltaGauge = registry.get("report.quality.mismatch.abs_delta").gauge().value();
        double runs = registry.get("report.quality.check.runs").counter().count();

        assertThat(daysGauge).isEqualTo(0.0);
        assertThat(rowsGauge).isEqualTo(0.0);
        assertThat(absDeltaGauge).isEqualTo(0.0);
        assertThat(runs).isEqualTo(1.0);
    }

    private MaintenanceDailyStatusCount stubRow(LocalDate day, MaintenanceStatus status, long count) {
        return new MaintenanceDailyStatusCount() {
            @Override
            public LocalDate getDay() { return day; }

            @Override
            public MaintenanceStatus getStatus() { return status; }

            @Override
            public long getCount() { return count; }
        };
    }
}
