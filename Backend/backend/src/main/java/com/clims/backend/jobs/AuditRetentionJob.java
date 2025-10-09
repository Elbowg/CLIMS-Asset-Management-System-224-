package com.clims.backend.jobs;

import com.clims.backend.config.AuditRetentionProperties;
import com.clims.backend.repository.AuditEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class AuditRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(AuditRetentionJob.class);

    private final AuditEventRepository repository;
    private final AuditRetentionProperties properties;
    private final MeterRegistry meterRegistry;

    public AuditRetentionJob(AuditEventRepository repository,
                             AuditRetentionProperties properties,
                             MeterRegistry meterRegistry) {
        this.repository = repository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "${audit.retention.schedule.cron:0 0 3 * * *}")
    @Transactional
    public void purgeOld() {
        int days = properties.getRetentionDays();
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
    int deleted = repository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Audit retention purge removed {} rows older than {} days", deleted, days);
        }
        meterRegistry.counter("audit.retention.purged")
                .increment(deleted);
    }
}
