package com.clims.backend.service.reporting;

import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.summary.AssetStatusDaily;
import com.clims.backend.model.summary.AssetStatusDailyId;
import com.clims.backend.model.summary.AuditActionDaily;
import com.clims.backend.model.summary.AuditActionDailyId;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AuditEventRepository;
import com.clims.backend.model.summary.MaintenanceActivityDaily;
import com.clims.backend.model.summary.MaintenanceActivityDailyId;
import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.repository.projection.MaintenanceDailyStatusCount;
import com.clims.backend.repository.summary.MaintenanceActivityDailyRepository;
import com.clims.backend.repository.summary.AssetStatusDailyRepository;
import com.clims.backend.repository.summary.AuditActionDailyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportingEtlService {
    private static final Logger log = LoggerFactory.getLogger(ReportingEtlService.class);
    public static final int MAX_WINDOW_DAYS = 366;

    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final AuditEventRepository auditEventRepository;
    private final MaintenanceActivityDailyRepository summaryRepository;
    private final AssetStatusDailyRepository assetStatusDailyRepository;
    private final AuditActionDailyRepository auditActionDailyRepository;
    private final MeterRegistry meterRegistry;

    public ReportingEtlService(MaintenanceRepository maintenanceRepository,
                               AssetRepository assetRepository,
                               AuditEventRepository auditEventRepository,
                               MaintenanceActivityDailyRepository summaryRepository,
                               AssetStatusDailyRepository assetStatusDailyRepository,
                               AuditActionDailyRepository auditActionDailyRepository,
                               MeterRegistry meterRegistry) {
        this.maintenanceRepository = maintenanceRepository;
        this.assetRepository = assetRepository;
        this.auditEventRepository = auditEventRepository;
        this.summaryRepository = summaryRepository;
        this.assetStatusDailyRepository = assetStatusDailyRepository;
        this.auditActionDailyRepository = auditActionDailyRepository;
        this.meterRegistry = meterRegistry;
    }

    public record EtlRunResult(LocalDate from, LocalDate to, int rowsDeleted, int rowsInserted) {}

    @Transactional
    public EtlRunResult backfillMaintenanceActivityDaily(LocalDate from, LocalDate to) {
        validateWindow(from, to);

        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.atTime(LocalTime.MAX);

        // 1) Fetch aggregated counts from source
        List<MaintenanceDailyStatusCount> aggregated = maintenanceRepository.findDailyStatusCounts(fromTs, toTs);

        // 2) Delete existing summary rows in window for idempotency
        int deleted = summaryRepository.deleteByBucketDateBetween(from, to);

        // 3) Insert new summary rows
        List<MaintenanceActivityDaily> toSave = new ArrayList<>();
        for (MaintenanceDailyStatusCount row : aggregated) {
            LocalDate day = row.getDay();
            MaintenanceStatus status = row.getStatus();
            long count = row.getCount();
            MaintenanceActivityDailyId id = new MaintenanceActivityDailyId(day, status);
            toSave.add(new MaintenanceActivityDaily(id, count));
        }
        summaryRepository.saveAll(toSave);

        meterRegistry.counter("reporting.etl.maintenance_activity_daily.rows_inserted").increment(toSave.size());
        meterRegistry.counter("reporting.etl.maintenance_activity_daily.rows_deleted").increment(deleted);
        log.info("ETL maintenance_activity_daily backfill from {} to {} inserted={} deleted={}", from, to, toSave.size(), deleted);

        return new EtlRunResult(from, to, deleted, toSave.size());
    }

    @Transactional
    public EtlRunResult backfillAssetStatusDaily(LocalDate from, LocalDate to) {
        validateWindow(from, to);
    var aggregated = assetRepository.findDailyStatusCounts(from, to);
        int deleted = assetStatusDailyRepository.deleteByBucketDateBetween(from, to);

        List<AssetStatusDaily> toSave = new ArrayList<>();
        for (var row : aggregated) {
            LocalDate day = row.getDay();
            AssetStatus status = row.getStatus();
            long count = row.getCount();
            AssetStatusDailyId id = new AssetStatusDailyId(day, status);
            toSave.add(new AssetStatusDaily(id, count));
        }
        assetStatusDailyRepository.saveAll(toSave);

        meterRegistry.counter("reporting.etl.asset_status_daily.rows_inserted").increment(toSave.size());
        meterRegistry.counter("reporting.etl.asset_status_daily.rows_deleted").increment(deleted);
        log.info("ETL asset_status_daily backfill from {} to {} inserted={} deleted={}", from, to, toSave.size(), deleted);
        return new EtlRunResult(from, to, deleted, toSave.size());
    }

    @Transactional
    public EtlRunResult backfillAuditActionDaily(LocalDate from, LocalDate to) {
        validateWindow(from, to);
        var fromTs = from.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        var toTs = to.atTime(LocalTime.MAX).toInstant(java.time.ZoneOffset.UTC);

        var aggregated = auditEventRepository.findDailyActionCounts(fromTs, toTs);
        int deleted = auditActionDailyRepository.deleteByBucketDateBetween(from, to);

        List<AuditActionDaily> toSave = new ArrayList<>();
        for (var row : aggregated) {
            LocalDate day = row.getDay();
            String action = row.getAction();
            long count = row.getCount();
            AuditActionDailyId id = new AuditActionDailyId(day, action);
            toSave.add(new AuditActionDaily(id, count));
        }
        auditActionDailyRepository.saveAll(toSave);

        meterRegistry.counter("reporting.etl.audit_action_daily.rows_inserted").increment(toSave.size());
        meterRegistry.counter("reporting.etl.audit_action_daily.rows_deleted").increment(deleted);
        log.info("ETL audit_action_daily backfill from {} to {} inserted={} deleted={}", from, to, toSave.size(), deleted);
        return new EtlRunResult(from, to, deleted, toSave.size());
    }

    private void validateWindow(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to must be provided");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must be on/after from");
        }
        long days = Duration.between(from.atStartOfDay(), to.atStartOfDay()).toDays() + 1;
        if (days > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException("window too large");
        }
    }
}
