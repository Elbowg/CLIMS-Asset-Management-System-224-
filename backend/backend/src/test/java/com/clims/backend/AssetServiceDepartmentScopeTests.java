package com.clims.backend;

import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.security.Role;
import com.clims.backend.services.AssetService;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.repositories.LocationRepository;
import com.clims.backend.repositories.VendorRepository;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

class AssetServiceDepartmentScopeTests {

    AssetService service;

    @Mock
    AssetRepository assetRepository;
    @Mock
    LocationRepository locationRepository;
    @Mock
    VendorRepository vendorRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    AppUserRepository userRepository;
    @Mock
    AuditLogService auditLogService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new AssetService(assetRepository, locationRepository, vendorRepository, departmentRepository, userRepository, auditLogService, new ModelMapper());
    }

    @Test
    void manager_cannot_update_asset_in_other_department() {
        Department d1 = new Department(); d1.setId(1L); d1.setName("D1");
        Department d2 = new Department(); d2.setId(2L); d2.setName("D2");

        AppUser manager = new AppUser(); manager.setId(10L); manager.setRole(Role.MANAGER); manager.setDepartment(d1);
        Asset a = new Asset(); a.setId(100L); a.setDepartment(d2);

        when(assetRepository.findById(100L)).thenReturn(java.util.Optional.of(a));

        assertThatThrownBy(() -> service.update(100L, new com.clims.backend.dto.AssetDtos.UpdateAssetRequest(null,null,null,null,null,null), manager)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void admin_can_update_any_asset() {
        Department d2 = new Department(); d2.setId(2L); d2.setName("D2");
        AppUser admin = new AppUser(); admin.setId(1L); admin.setRole(Role.ADMIN);
        Asset a = new Asset(); a.setId(200L); a.setDepartment(d2);
        when(assetRepository.findById(200L)).thenReturn(java.util.Optional.of(a));
        when(assetRepository.save(any())).thenReturn(a);

        service.update(200L, new com.clims.backend.dto.AssetDtos.UpdateAssetRequest(null,null,null,null,null,null), admin);

        verify(assetRepository, times(1)).save(a);
    }
}
