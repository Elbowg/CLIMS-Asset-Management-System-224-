package com.clims.backend.services;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.AuditLog;
import com.clims.backend.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityName, Long entityId, String action, String details, AppUser user) {
        AuditLog log = new AuditLog();
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        log.setUser(user);
        auditLogRepository.save(log);
    }
}
