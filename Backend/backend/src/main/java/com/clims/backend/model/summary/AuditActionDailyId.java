package com.clims.backend.model.summary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class AuditActionDailyId implements Serializable {
    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    @Column(name = "audit_action", nullable = false, length = 64)
    private String auditAction;

    public AuditActionDailyId() {}

    public AuditActionDailyId(LocalDate bucketDate, String auditAction) {
        this.bucketDate = bucketDate;
        this.auditAction = auditAction;
    }

    public LocalDate getBucketDate() { return bucketDate; }
    public void setBucketDate(LocalDate bucketDate) { this.bucketDate = bucketDate; }
    public String getAuditAction() { return auditAction; }
    public void setAuditAction(String auditAction) { this.auditAction = auditAction; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditActionDailyId that = (AuditActionDailyId) o;
        return Objects.equals(bucketDate, that.bucketDate) && Objects.equals(auditAction, that.auditAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketDate, auditAction);
    }
}
