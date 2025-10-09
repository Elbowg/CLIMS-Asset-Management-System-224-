package com.clims.backend.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant timestamp;
    private String principal;
    private String action; // e.g. LOGIN, LOGOUT, REFRESH
    private String details; // optional context
    private String ip;
    private String requestId;

    @PrePersist
    void prePersist() {
        if (timestamp == null) timestamp = Instant.now();
    }

    public AuditEvent() {}
    public AuditEvent(String principal, String action, String details, String ip, String requestId) {
        this.principal = principal;
        this.action = action;
        this.details = details;
        this.ip = ip;
        this.requestId = requestId;
    }
    // Test/support constructor allowing explicit timestamp (used for retention tests)
    public AuditEvent(Instant timestamp, String principal, String action, String details, String ip, String requestId) {
        this.timestamp = timestamp;
        this.principal = principal;
        this.action = action;
        this.details = details;
        this.ip = ip;
        this.requestId = requestId;
    }

    // getters (no setters for immutability preference except JPA needs) 
    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getPrincipal() { return principal; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public String getIp() { return ip; }
    public String getRequestId() { return requestId; }
}
