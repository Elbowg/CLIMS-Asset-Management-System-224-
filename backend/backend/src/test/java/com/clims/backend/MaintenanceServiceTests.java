package com.clims.backend;

import com.clims.backend.dto.MaintenanceDtos;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.enums.MaintenanceStatus;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.MaintenanceRepository;
import com.clims.backend.services.AuditLogService;
import com.clims.backend.services.MaintenanceService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class MaintenanceServiceTests {

    @Test
    void schedule_setsUnderRepair_andLogs() {
        MaintenanceRepository maintRepo = Mockito.mock(MaintenanceRepository.class);
        AssetRepository assetRepo = Mockito.mock(AssetRepository.class);
        AuditLogService audit = Mockito.mock(AuditLogService.class);

        Asset asset = new Asset();
        asset.setId(10L);
        asset.setStatus(AssetStatus.AVAILABLE);
        Mockito.when(assetRepo.findById(10L)).thenReturn(Optional.of(asset));
        Mockito.when(maintRepo.save(any(Maintenance.class))).thenAnswer(inv -> {
            Maintenance m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });

        MaintenanceService svc = new MaintenanceService(maintRepo, assetRepo, audit);
        MaintenanceDtos.CreateRequest req = new MaintenanceDtos.CreateRequest(10L, "Fan cleaning", LocalDate.now());
        AppUser actor = new AppUser();
        actor.setUsername("tech");

        Maintenance saved = svc.schedule(req, actor);
        Assertions.assertEquals(99L, saved.getId());
        Assertions.assertEquals(AssetStatus.UNDER_REPAIR, asset.getStatus());
        Mockito.verify(audit).log(Mockito.eq("Maintenance"), Mockito.eq(99L), Mockito.eq("CREATE"), Mockito.anyString(), Mockito.eq(actor));
    }

    @Test
    void updateStatus_completed_setsAssetAvailable() {
        MaintenanceRepository maintRepo = Mockito.mock(MaintenanceRepository.class);
        AssetRepository assetRepo = Mockito.mock(AssetRepository.class);
        AuditLogService audit = Mockito.mock(AuditLogService.class);

        Asset asset = new Asset();
        asset.setId(7L);
        asset.setStatus(AssetStatus.UNDER_REPAIR);
        Maintenance m = new Maintenance();
        m.setId(5L);
        m.setAsset(asset);
        Mockito.when(maintRepo.findById(5L)).thenReturn(Optional.of(m));
        Mockito.when(maintRepo.save(any(Maintenance.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceService svc = new MaintenanceService(maintRepo, assetRepo, audit);
        MaintenanceDtos.UpdateStatusRequest req = new MaintenanceDtos.UpdateStatusRequest(MaintenanceStatus.COMPLETED, LocalDate.now());
        AppUser actor = new AppUser();

        Maintenance saved = svc.updateStatus(5L, req, actor);
        Assertions.assertEquals(MaintenanceStatus.COMPLETED, saved.getStatus());
        Assertions.assertEquals(AssetStatus.AVAILABLE, asset.getStatus());
        Mockito.verify(audit).log(Mockito.eq("Maintenance"), Mockito.eq(5L), Mockito.eq("UPDATE"), Mockito.anyString(), Mockito.eq(actor));
    }

    @Test
    void schedule_missingAsset_throwsNotFound() {
        MaintenanceService svc = new MaintenanceService(
                Mockito.mock(MaintenanceRepository.class),
                Mockito.mock(AssetRepository.class),
                Mockito.mock(AuditLogService.class)
        );
        MaintenanceDtos.CreateRequest req = new MaintenanceDtos.CreateRequest(999L, "Bad", LocalDate.now());
        Assertions.assertThrows(NotFoundException.class, () -> svc.schedule(req, new AppUser()));
    }

    @Test
    void update_missing_throwsNotFound() {
        MaintenanceService svc = new MaintenanceService(
                Mockito.mock(MaintenanceRepository.class),
                Mockito.mock(AssetRepository.class),
                Mockito.mock(AuditLogService.class)
        );
        MaintenanceDtos.UpdateStatusRequest req = new MaintenanceDtos.UpdateStatusRequest(MaintenanceStatus.IN_PROGRESS, null);
        Assertions.assertThrows(NotFoundException.class, () -> svc.updateStatus(123L, req, new AppUser()));
    }
}
