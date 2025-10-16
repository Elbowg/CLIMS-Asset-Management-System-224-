package com.clims.backend;

import com.clims.backend.controllers.AssetController;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.services.AssetService;
import com.clims.backend.services.AuditLogService;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.security.Role;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Import;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.MaintenanceStatus;

import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssetController.class)
@Import(GlobalExceptionHandler.class)
class AssetControllerSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    AssetService assetService;

    @MockBean
    com.clims.backend.services.MaintenanceService maintenanceService;

    @MockBean
    AuditLogService auditLogService;

    @MockBean
    CurrentUserService currentUserService;

    // Provide missing dependencies for the controller slice
    @MockBean
    ModelMapper modelMapper;

    // Mock security filter and util to avoid full security wiring in MVC slice
    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @MockBean
    JwtUtil jwtUtil;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void list_requiresAuth() throws Exception {
        mvc.perform(get("/api/assets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withAdmin_ok() throws Exception {
    given(assetService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0,10), 0));
        mvc.perform(get("/api/assets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withPaging_structure_ok() throws Exception {
    Asset a1 = new Asset(); a1.setId(1L); a1.setAssetTag("TAG-1");
    Asset a2 = new Asset(); a2.setId(2L); a2.setAssetTag("TAG-2");
    given(assetService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(java.util.List.of(a1,a2), PageRequest.of(1,2), 12));

    mvc.perform(get("/api/assets").param("page","1").param("size","2").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content").isArray())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(12))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalPages").value(6))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.page").value(1))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.size").value(2));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withParams_mapsPageableAndFilters() throws Exception {
    given(assetService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(2,5, Sort.by("make").ascending()), 0));

    mvc.perform(get("/api/assets")
            .param("page","2")
            .param("size","5")
            .param("sort","make,asc")
            .param("status","ASSIGNED")
            .param("departmentId","10")
            .param("locationId","20")
            .param("vendorId","30")
            .param("q","dell")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    ArgumentCaptor<AssetStatus> statusCaptor = ArgumentCaptor.forClass(AssetStatus.class);
    ArgumentCaptor<Long> depCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> locCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> venCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);

    verify(assetService).search(pageableCaptor.capture(), statusCaptor.capture(), depCaptor.capture(), locCaptor.capture(), venCaptor.capture(), qCaptor.capture());

    var pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(2);
    assertThat(pageable.getPageSize()).isEqualTo(5);
    assertThat(pageable.getSort().getOrderFor("make").getDirection()).isEqualTo(Sort.Direction.ASC);
    assertThat(statusCaptor.getValue()).isEqualTo(AssetStatus.ASSIGNED);
    assertThat(depCaptor.getValue()).isEqualTo(10L);
    assertThat(locCaptor.getValue()).isEqualTo(20L);
    assertThat(venCaptor.getValue()).isEqualTo(30L);
    assertThat(qCaptor.getValue()).isEqualTo("dell");
    }

    @Test
    void getById_requiresAuth() throws Exception {
        mvc.perform(get("/api/assets/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getById_withAdmin_ok() throws Exception {
        Asset a = new Asset();
        a.setId(1L);
        a.setAssetTag("ASSET-001");
        a.setSerialNumber("SN");
        a.setMake("Dell");
        a.setModel("XPS");
        given(assetService.get(1L)).willReturn(a);

        mvc.perform(get("/api/assets/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void create_manager_forbidden_when_service_denies() throws Exception {
        AppUser mgr = new AppUser(); mgr.setId(10L); mgr.setRole(Role.MANAGER);
        given(currentUserService.requireCurrentUser()).willReturn(mgr);
        given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willThrow(new org.springframework.security.access.AccessDeniedException("Forbidden"));

        String payload = "{\"serialNumber\":\"SN123\",\"make\":\"Dell\",\"model\":\"X\",\"purchaseDate\":\"2024-01-01\",\"departmentId\":2}";
        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void delete_requiresAdmin() throws Exception {
        // Controller-level PreAuthorize should block non-admins before service is called
        mvc.perform(delete("/api/assets/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void admin_can_create_update_assign_dispose() throws Exception {
        // create
    String payload = "{\"serialNumber\":\"SN1\",\"make\":\"Dell\",\"model\":\"X\",\"purchaseDate\":\"2024-01-01\"}";
    given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());

        // update
    given(assetService.get(1L)).willReturn(new Asset());
    given(assetService.update(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(put("/api/assets/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        // assign
    given(assetService.assign(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(post("/api/assets/1/assign").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":2}")).andExpect(status().isOk());

        // dispose
    given(assetService.get(2L)).willReturn(new Asset());
    given(assetService.dispose(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(post("/api/assets/2/dispose").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"IT_STAFF"})
    void itStaff_can_create_and_modify() throws Exception {
    String payload = "{\"serialNumber\":\"SN2\",\"make\":\"HP\",\"model\":\"P\",\"purchaseDate\":\"2024-01-01\"}";
    given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"MANAGER"})
    void manager_allowed_within_department_but_forbidden_for_other() throws Exception {
        Department d1 = new Department(); d1.setId(1L);
        Department d2 = new Department(); d2.setId(2L);
        AppUser mgr = new AppUser(); mgr.setId(10L); mgr.setRole(Role.MANAGER); mgr.setDepartment(d1);
        given(currentUserService.requireCurrentUser()).willReturn(mgr);

        // creating in own dept allowed
    String payloadOwn = "{\"serialNumber\":\"SN3\",\"make\":\"Lenovo\",\"model\":\"T\",\"purchaseDate\":\"2024-01-01\",\"departmentId\":1}";
        given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(new Asset());
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payloadOwn)).andExpect(status().isOk());

        // creating in other dept forbidden
    String payloadOther = "{\"serialNumber\":\"SN4\",\"make\":\"Lenovo\",\"model\":\"T\",\"purchaseDate\":\"2024-01-01\",\"departmentId\":2}";
        given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willThrow(new org.springframework.security.access.AccessDeniedException("Forbidden"));
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payloadOther)).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getById_notFound_returns404() throws Exception {
        given(assetService.get(99L)).willThrow(new NotFoundException("Asset not found"));

        mvc.perform(get("/api/assets/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getQr_requiresAuth() throws Exception {
        mvc.perform(get("/api/assets/ASSET-1/qr")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getQr_returnsPngBytes() throws Exception {
        byte[] png = new byte[]{1,2,3};
        given(assetService.generateQrPng("ASSET-1")).willReturn(png);

        mvc.perform(get("/api/assets/ASSET-1/qr"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Type", org.springframework.http.MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getQr_dataUrl_mode_returnsString() throws Exception {
        byte[] png = new byte[]{1,2,3};
        given(assetService.generateQrPng("ASSET-1")).willReturn(png);

        mvc.perform(get("/api/assets/ASSET-1/qr").param("dataUrl","true"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test
    void maintenanceHistory_requiresAuth() throws Exception {
        mvc.perform(get("/api/assets/1/maintenance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void maintenanceHistory_assetNotFound_returns404() throws Exception {
        given(assetService.get(123L)).willThrow(new NotFoundException("Asset not found"));
        mvc.perform(get("/api/assets/123/maintenance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void maintenanceHistory_withParams_mapsPageableAndFilters() throws Exception {
        Maintenance m1 = new Maintenance(); m1.setId(1L);
        Page<Maintenance> page = new PageImpl<>(java.util.List.of(m1), PageRequest.of(2, 5, Sort.by("scheduledDate").ascending()), 11);
        given(assetService.get(1L)).willReturn(new Asset());
        given(maintenanceService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .willReturn(page);

        mvc.perform(get("/api/assets/1/maintenance")
                .param("page","2")
                .param("size","5")
                .param("sort","scheduledDate,asc")
                .param("status","COMPLETED")
                .param("dateFrom","2024-01-01")
                .param("dateTo","2024-12-31")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(11))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.page").value(2))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.size").value(5));

        ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        ArgumentCaptor<MaintenanceStatus> statusCaptor = ArgumentCaptor.forClass(MaintenanceStatus.class);
        ArgumentCaptor<Long> assetIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<java.time.LocalDate> fromCaptor = ArgumentCaptor.forClass(java.time.LocalDate.class);
        ArgumentCaptor<java.time.LocalDate> toCaptor = ArgumentCaptor.forClass(java.time.LocalDate.class);
        verify(maintenanceService).search(pageableCaptor.capture(), statusCaptor.capture(), assetIdCaptor.capture(), fromCaptor.capture(), toCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("scheduledDate").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(statusCaptor.getValue()).isEqualTo(MaintenanceStatus.COMPLETED);
        assertThat(assetIdCaptor.getValue()).isEqualTo(1L);
        assertThat(fromCaptor.getValue()).isEqualTo(java.time.LocalDate.parse("2024-01-01"));
        assertThat(toCaptor.getValue()).isEqualTo(java.time.LocalDate.parse("2024-12-31"));
    }
}
