package com.clims.backend.services;

import com.clims.backend.dto.ReportDtos;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.MaintenanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ReportServiceTest {

    AssetRepository assetRepository;
    MaintenanceRepository maintenanceRepository;
    ReportService reportService;

    @BeforeEach
    public void setup() {
        assetRepository = Mockito.mock(AssetRepository.class);
        maintenanceRepository = Mockito.mock(MaintenanceRepository.class);
        reportService = new ReportService(assetRepository, maintenanceRepository);
    }

    @Test
    public void computeKpis_countsAndGroupsAreReturned() {
        when(assetRepository.count()).thenReturn(3L);

    // Simulate grouped counts: 2 AVAILABLE, 1 ASSIGNED
        List<Object[]> groups = new ArrayList<>();
    groups.add(new Object[]{AssetStatus.AVAILABLE, 2L});
    groups.add(new Object[]{AssetStatus.ASSIGNED, 1L});
        when(assetRepository.countByStatusGroup()).thenReturn(groups);

        LocalDate today = LocalDate.now();
        when(maintenanceRepository.countUpcomingFrom(any(LocalDate.class))).thenReturn(5L);

        ReportDtos.KpiResponse r = reportService.computeKpis();

        assertEquals(3L, r.totalAssets());
        HashMap<String, Long> expected = new HashMap<>();
    expected.put(AssetStatus.AVAILABLE.name(), 2L);
    expected.put(AssetStatus.ASSIGNED.name(), 1L);
        // other statuses should be present with 0 or be absent depending on initialization; check keys we set
    assertEquals(2L, r.assetsByStatus().get(AssetStatus.AVAILABLE.name()));
    assertEquals(1L, r.assetsByStatus().get(AssetStatus.ASSIGNED.name()));
        assertEquals(5L, r.upcomingMaintenance());
    }

    @Test
    public void computeKpis_handlesEmptyGroups() {
        when(assetRepository.count()).thenReturn(0L);
        when(assetRepository.countByStatusGroup()).thenReturn(new ArrayList<>());
        when(maintenanceRepository.countUpcomingFrom(any(LocalDate.class))).thenReturn(0L);

        ReportDtos.KpiResponse r = reportService.computeKpis();
        assertEquals(0L, r.totalAssets());
        // All known statuses should be present (initialized to 0)
        for (com.clims.backend.models.enums.AssetStatus s : com.clims.backend.models.enums.AssetStatus.values()) {
            // use getOrDefault in case map doesn't include it
            assertEquals(0L, r.assetsByStatus().getOrDefault(s.name(), 0L));
        }
        assertEquals(0L, r.upcomingMaintenance());
    }

    @Test
    public void computeKpis_handlesNonNumericAndNullStatus() {
        when(assetRepository.count()).thenReturn(2L);
        List<Object[]> groups = new ArrayList<>();
        groups.add(new Object[]{null, "3"}); // null status should map to UNKNOWN and parse string count
    groups.add(new Object[]{com.clims.backend.models.enums.AssetStatus.AVAILABLE,  -1}); // negative test value
        when(assetRepository.countByStatusGroup()).thenReturn(groups);
        when(maintenanceRepository.countUpcomingFrom(any(LocalDate.class))).thenReturn(7L);

        ReportDtos.KpiResponse r = reportService.computeKpis();
        // null status becomes UNKNOWN with parsed value 3
        assertEquals(3L, r.assetsByStatus().getOrDefault("UNKNOWN", 0L));
    // AVAILABLE should be updated to -1 (as parsed long)
    assertEquals(-1L, r.assetsByStatus().getOrDefault(com.clims.backend.models.enums.AssetStatus.AVAILABLE.name(), 0L));
        assertEquals(7L, r.upcomingMaintenance());
    }
}
