package com.clims.backend.service;

import com.clims.backend.exception.BusinessRuleException;
import com.clims.backend.model.Maintenance;
import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.repository.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceLifecycleTest {
    MaintenanceRepository repo;
    MaintenanceService service;

    @BeforeEach
    void setup() {
        repo = mock(MaintenanceRepository.class);
        service = new MaintenanceService(repo);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Maintenance newReported() {
        Maintenance m = new Maintenance();
        m.setStatus(MaintenanceStatus.REPORTED);
        m.setReportedAt(LocalDateTime.now());
        return m;
    }

    @Test
    void reportedToInProgress() {
        Maintenance m = newReported();
        service.startProgress(m);
        assertEquals(MaintenanceStatus.IN_PROGRESS, m.getStatus());
    }

    @Test
    void inProgressToResolved() {
        Maintenance m = newReported();
        service.startProgress(m);
        service.resolve(m, "Fixed");
        assertEquals(MaintenanceStatus.RESOLVED, m.getStatus());
    }

    @Test
    void resolvedCannotGoBack() {
        Maintenance m = newReported();
        service.startProgress(m);
        service.resolve(m, "Fixed");
        assertThrows(BusinessRuleException.class, () -> service.startProgress(m));
    }

    @Test
    void cancelFromReported() {
        Maintenance m = newReported();
        service.cancel(m);
        assertEquals(MaintenanceStatus.CANCELLED, m.getStatus());
    }
}
