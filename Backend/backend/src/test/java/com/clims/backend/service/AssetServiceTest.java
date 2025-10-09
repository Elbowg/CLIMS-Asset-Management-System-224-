package com.clims.backend.service;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.AssignmentHistory;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AssignmentHistoryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
// ...existing imports...

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.clims.backend.service.OutboxEventService;

class AssetServiceTest {

    AssetRepository assetRepo;
    AssignmentHistoryRepository historyRepo;
    AssetService service;
    OutboxEventService outboxEventService;

    @BeforeEach
    void setup() {
        assetRepo = mock(AssetRepository.class);
        historyRepo = mock(AssignmentHistoryRepository.class);
    outboxEventService = mock(OutboxEventService.class);
    service = new AssetService(assetRepo, historyRepo, outboxEventService);
    }

    @Test
    void assignAndUnassign() {
        Asset a = new Asset(); a.setId(1L); a.setStatus(AssetStatus.AVAILABLE);
        User u = new User(); u.setId(2L);

        when(assetRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Asset assigned = service.assignToUser(a, u);
        assertEquals(AssetStatus.ASSIGNED, assigned.getStatus());
        assertEquals(u, assigned.getAssignedUser());

        // simulate history returned
        AssignmentHistory h = new AssignmentHistory(); h.setId(5L); h.setAsset(a); h.setUser(u);
        when(historyRepo.findTopByAsset_IdOrderByAssignedAtDesc(1L)).thenReturn(Optional.of(h));

        Asset unassigned = service.unassignFromUser(assigned);
        assertEquals(AssetStatus.AVAILABLE, unassigned.getStatus());
        assertNull(unassigned.getAssignedUser());
    verify(historyRepo, times(2)).save(any(AssignmentHistory.class));
    verifyNoInteractions(outboxEventService); // create() path not exercised here
    }
}
