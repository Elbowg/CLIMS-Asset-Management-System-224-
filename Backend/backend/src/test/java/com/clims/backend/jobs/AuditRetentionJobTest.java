package com.clims.backend.jobs;

import com.clims.backend.config.AuditRetentionProperties;
import com.clims.backend.model.AuditEvent;
import com.clims.backend.repository.AuditEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AuditRetentionJob.class, AuditRetentionProperties.class, SimpleMeterRegistry.class})
class AuditRetentionJobTest {

    @Autowired
    AuditEventRepository repository;
    @Autowired
    AuditRetentionJob job;
    @Autowired
    AuditRetentionProperties props;
    @Autowired
    SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        props.setRetentionDays(30); // shorten for test
    }

    @Test
    void purgesEventsOlderThanRetention() {
        Instant now = Instant.now();
        // Older than 30 days
        repository.save(new AuditEvent(now.minus(40, ChronoUnit.DAYS), "alice", "LOGIN", "old", "127.0.0.1", "r-old"));
        // Within retention window
        repository.save(new AuditEvent(now.minus(10, ChronoUnit.DAYS), "bob", "LOGIN", "recent", "127.0.0.1", "r-new"));

        assertThat(repository.count()).isEqualTo(2);
        job.purgeOld();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAll()).allMatch(ev -> ev.getRequestId().equals("r-new"));

        double purged = meterRegistry.find("audit.retention.purged").counter().count();
        assertThat(purged).isEqualTo(1.0d);
    }
}
