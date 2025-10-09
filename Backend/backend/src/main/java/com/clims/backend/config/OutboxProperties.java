package com.clims.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "outbox.dispatch")
public class OutboxProperties {
    /** Fixed delay between dispatcher polls (ms). */
    private long intervalMs = 5000;
    /** Maximum attempts before marking FAILED. */
    private int maxAttempts = 5;
    /** Initial backoff in milliseconds. */
    private long initialBackoffMs = 500;
    /** Multiplier applied per retry. */
    private double backoffMultiplier = 2.0;
    /** Max batch size fetched per poll (requires repository support). */
    private int batchSize = 50; // future: limit query size

    public long getIntervalMs() { return intervalMs; }
    public void setIntervalMs(long intervalMs) { this.intervalMs = intervalMs; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public long getInitialBackoffMs() { return initialBackoffMs; }
    public void setInitialBackoffMs(long initialBackoffMs) { this.initialBackoffMs = initialBackoffMs; }
    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
}
