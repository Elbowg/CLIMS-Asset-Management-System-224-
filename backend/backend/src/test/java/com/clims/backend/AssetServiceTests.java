package com.clims.backend;

import com.clims.backend.dto.AssetDtos;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.LocationRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.repositories.VendorRepository;
import com.clims.backend.services.AssetService;
import com.clims.backend.services.AuditLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class AssetServiceTests {

    @Test
    void createAsset_happyPath() {
        AssetRepository assetRepo = Mockito.mock(AssetRepository.class);
        LocationRepository locRepo = Mockito.mock(LocationRepository.class);
        VendorRepository vendorRepo = Mockito.mock(VendorRepository.class);
    AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
    DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        AuditLogService audit = Mockito.mock(AuditLogService.class);
        ModelMapper mapper = new ModelMapper();

        Mockito.when(assetRepo.save(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

    AssetService svc = new AssetService(assetRepo, locRepo, vendorRepo, deptRepo, userRepo, audit, mapper);
    AssetDtos.CreateAssetRequest req = new AssetDtos.CreateAssetRequest(
        "SN-1", "Dell", "XPS", LocalDate.now(), null, null, null, null
    );
        AppUser actor = new AppUser();
        actor.setUsername("tester");

        Asset saved = svc.create(req, actor);
        Assertions.assertNotNull(saved.getId());
        Assertions.assertNotNull(saved.getAssetTag());
    }

    @Test
    void get_missingAsset_throwsNotFound() {
        AssetRepository assetRepo = Mockito.mock(AssetRepository.class);
        Mockito.when(assetRepo.findById(123L)).thenReturn(Optional.empty());
    AssetService svc = new AssetService(assetRepo, Mockito.mock(LocationRepository.class), Mockito.mock(VendorRepository.class), Mockito.mock(DepartmentRepository.class), Mockito.mock(AppUserRepository.class), Mockito.mock(AuditLogService.class), new ModelMapper());
        Assertions.assertThrows(NotFoundException.class, () -> svc.get(123L));
    }
}
