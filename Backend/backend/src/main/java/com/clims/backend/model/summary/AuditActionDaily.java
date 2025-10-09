package com.clims.backend.model.summary;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_action_daily")
public class AuditActionDaily {

    @EmbeddedId
    private AuditActionDailyId id;

    @Column(name = "action_count", nullable = false)
    private long actionCount;

    public AuditActionDaily() {}

    public AuditActionDaily(AuditActionDailyId id, long actionCount) {
        this.id = id;
        this.actionCount = actionCount;
    }

    public AuditActionDailyId getId() { return id; }
    public void setId(AuditActionDailyId id) { this.id = id; }
    public long getActionCount() { return actionCount; }
    public void setActionCount(long actionCount) { this.actionCount = actionCount; }
}
