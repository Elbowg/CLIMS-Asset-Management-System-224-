package com.clims.backend.dispatch;

import com.clims.backend.model.OutboxEvent;
import com.clims.backend.repository.OutboxEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.clims.backend.config.OutboxProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import com.clims.backend.model.OutboxEvent.Status;
import io.micrometer.core.instrument.Timer;

/**
 * Simple polling dispatcher (Phase 3 initial skeleton) that:
 * - fetches NEW / RETRY events due for dispatch
 * - attempts in-process publish (currently just logs) and marks SENT
 * - on exception applies exponential backoff and RETRY, eventually FAILED
 */
@Component
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository repo;
    private final MeterRegistry meterRegistry;

    private final OutboxProperties properties;
    private final java.util.Map<String, OutboxEventHandler> handlerMap;

    public OutboxDispatcher(OutboxEventRepository repo,
                            MeterRegistry meterRegistry,
                            OutboxProperties properties,
                            java.util.List<OutboxEventHandler> handlers) {
        this.repo = repo;
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.handlerMap = new java.util.HashMap<>();
        for (OutboxEventHandler h : handlers) {
            this.handlerMap.put(h.getEventType(), h);
        }

        // Gauges for queue depth per status
        for (Status s : Status.values()) {
            meterRegistry.gauge("outbox.queue.depth", List.of(io.micrometer.core.instrument.Tag.of("status", s.name())), this,
                ref -> (double) ref.repo.countByStatus(s));
        }
    }

    // Use configured interval (ms) with safe default. Avoid null placeholder which caused scheduling exception in tests.
    @Scheduled(fixedDelayString = "${outbox.dispatch.interval-ms:5000}")
    @Transactional
    public void dispatchBatch() {
        Instant start = Instant.now();
    List<OutboxEvent> batch = repo.findDispatchBatch(Instant.now(), PageRequest.of(0, properties.getBatchSize()));
        if (batch.isEmpty()) return;
        meterRegistry.counter("outbox.dispatch.attempt").increment(batch.size());
        for (OutboxEvent e : batch) {
            try {
                OutboxEventHandler handler = handlerMap.get(e.getEventType());
                if (handler == null) {
                    // Missing handler treated as permanent failure
                    log.error("No handler registered for eventType={}, marking FAILED id={}", e.getEventType(), e.getId());
                    e.markFailedPermanent();
                    meterRegistry.counter("outbox.dispatch.missingHandler").increment();
                    continue;
                }
                handler.handle(e);
                e.markSent();
                meterRegistry.counter("outbox.dispatch.success").increment();
            } catch (Exception ex) {
                meterRegistry.counter("outbox.dispatch.failure", "exception", ex.getClass().getSimpleName()).increment();
                if (e.getAttemptCount() + 1 >= properties.getMaxAttempts()) {
                    e.markFailedPermanent();
                    meterRegistry.counter("outbox.dispatch.dead").increment();
                    log.error("Outbox event id={} permanently failed after attempts", e.getId(), ex);
                } else {
                    Instant next = computeNextAttempt(e.getAttemptCount());
                    e.markRetry(next);
                    log.warn("Outbox event id={} failed attempt={} scheduling retry at {}", e.getId(), e.getAttemptCount(), next, ex);
                }
            }
        }
        Timer.builder("outbox.dispatch.batch.time").register(meterRegistry)
            .record(Duration.between(start, Instant.now()));
    }

    private Instant computeNextAttempt(int currentAttempts) {
        double factor = Math.pow(properties.getBackoffMultiplier(), currentAttempts);
        long delayMs = Math.round(properties.getInitialBackoffMs() * factor);
        return Instant.now().plusMillis(delayMs);
    }
}
