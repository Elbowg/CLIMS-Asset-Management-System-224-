package com.clims.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "audit")
public class AuditRetentionProperties {
    /** Number of days to retain audit events. */
    private int retentionDays = 90; // default
    /** Cron expression for scheduling the purge job. */
    private String retentionScheduleCron = "0 0 3 * * *"; // daily at 3am

    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }

    public String getRetentionScheduleCron() { return retentionScheduleCron; }
    public void setRetentionScheduleCron(String retentionScheduleCron) { this.retentionScheduleCron = retentionScheduleCron; }
}
