package com.clims.backend.service;

import com.clims.backend.exception.BusinessRuleException;
import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AssignmentHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.clims.backend.service.OutboxEventService;

class AssetLifecycleTest {
    AssetRepository assetRepo;
    AssignmentHistoryRepository histRepo;
    AssetService service;
    OutboxEventService outboxEventService;

    @BeforeEach
    void setup() {
        assetRepo = mock(AssetRepository.class);
        histRepo = mock(AssignmentHistoryRepository.class);
    outboxEventService = mock(OutboxEventService.class);
    service = new AssetService(assetRepo, histRepo, outboxEventService);
        when(assetRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void validTransitionAvailableToAssigned() {
        Asset a = new Asset(); a.setStatus(AssetStatus.AVAILABLE); a.setId(5L);
        service.assignToUser(a, null);
        assertEquals(AssetStatus.ASSIGNED, a.getStatus());
        verifyNoInteractions(outboxEventService); // assign path doesn't record outbox yet
    }

    @Test
    void invalidTransitionRetiredToAssignedFails() {
        Asset a = new Asset(); a.setStatus(AssetStatus.RETIRED); a.setId(9L);
        assertThrows(BusinessRuleException.class, () -> service.assignToUser(a, null));
    }

    @Test
    void moveToMaintenanceFromAssigned() {
        Asset a = new Asset(); a.setStatus(AssetStatus.ASSIGNED); a.setId(3L);
        service.moveToMaintenance(a);
        assertEquals(AssetStatus.MAINTENANCE, a.getStatus());
    }

    @Test
    void retireFromMaintenance() {
        Asset a = new Asset(); a.setStatus(AssetStatus.MAINTENANCE); a.setId(11L);
        service.retire(a);
        assertEquals(AssetStatus.RETIRED, a.getStatus());
    }
}
