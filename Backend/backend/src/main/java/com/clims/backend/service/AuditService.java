package com.clims.backend.service;

import com.clims.backend.model.AuditEvent;
import com.clims.backend.repository.AuditEventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEventRepository repo;
    private final MeterRegistry meterRegistry;

    public AuditService(AuditEventRepository repo, MeterRegistry meterRegistry) {
        this.repo = repo;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public void record(String principal, String action, String details, String ip, String requestId) {
        try {
            repo.save(new AuditEvent(principal, action, details, ip, requestId));
            if (meterRegistry != null) meterRegistry.counter("audit.write.success").increment();
        } catch (Exception ex) {
            if (meterRegistry != null) meterRegistry.counter("audit.write.failure", "exception", ex.getClass().getSimpleName()).increment();
            throw ex;
        }
    }
}
