package com.clims.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    public enum Status { NEW, RETRY, SENT, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType; // e.g. Asset
    private String aggregateId; // string form (could be Long id)
    private String eventType; // e.g. AssetCreated
    @Column(columnDefinition = "LONGTEXT") // align with Flyway migration (LONGTEXT) to satisfy schema validation
    private String payload; // JSON serialized event payload

    @Enumerated(EnumType.STRING)
    private Status status;
    private int attemptCount;
    private Instant nextAttemptAt;
    private String correlationId;
    private String requestId;
    private int version = 1;
    private Instant createdAt;
    private Instant updatedAt;

    public OutboxEvent() {}

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload,
                       String correlationId, String requestId) {
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.status = Status.NEW;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = Status.NEW;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // getters
    public Long getId() { return id; }
    public String getAggregateType() { return aggregateType; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Status getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public String getCorrelationId() { return correlationId; }
    public String getRequestId() { return requestId; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // domain mutation helpers
    public void markRetry(Instant nextAttemptAt) {
        this.status = Status.RETRY;
        this.attemptCount += 1;
        this.nextAttemptAt = nextAttemptAt;
    }
    public void markSent() {
        this.status = Status.SENT;
        this.nextAttemptAt = null;
    }
    public void markFailedPermanent() {
        // When we permanently fail we count the in-flight attempt (mirrors retry path semantics)
        this.attemptCount += 1;
        this.status = Status.FAILED;
        this.nextAttemptAt = null;
    }
}
