package com.clims.backend.service;

import com.clims.backend.model.OutboxEvent;
import com.clims.backend.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);

    private final OutboxEventRepository repo;
    private final MeterRegistry meterRegistry;

    public OutboxEventService(OutboxEventRepository repo, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public OutboxEvent record(String aggregateType, String aggregateId, String eventType, String payload,
                              String correlationId, String requestId) {
        try {
            OutboxEvent e = new OutboxEvent(aggregateType, aggregateId, eventType, payload, correlationId, requestId);
            OutboxEvent saved = repo.save(e);
            meterRegistry.counter("outbox.insert.success").increment();
            return saved;
        } catch (RuntimeException ex) {
            meterRegistry.counter("outbox.insert.failure", "exception", ex.getClass().getSimpleName()).increment();
            log.error("Failed to record outbox event aggregateType={} eventType={}", aggregateType, eventType, ex);
            throw ex;
        }
    }
}
