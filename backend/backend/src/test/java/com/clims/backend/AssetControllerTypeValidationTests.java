package com.clims.backend;

import com.clims.backend.controllers.AssetController;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.services.AssetService;
import com.clims.backend.services.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.security.Role;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.enums.AssetType;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssetController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = {"ADMIN"})
class AssetControllerTypeValidationTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    AssetService assetService;

    @MockBean
    AuditLogService auditLogService;

    @MockBean
    com.clims.backend.services.MaintenanceService maintenanceService;

    @MockBean
    CurrentUserService currentUserService;

    @MockBean
    ModelMapper modelMapper;

    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    com.clims.backend.security.AssetSecurity assetSecurity;

    @MockBean
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        // Provide a current user so PreAuthorize expressions can evaluate during tests
        AppUser admin = new AppUser();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        given(currentUserService.requireCurrentUser()).willReturn(admin);
        given(assetSecurity.canCreate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(true);
    }

    @Test
    void create_missingType_returns400() throws Exception {
        String payload = "{\"serialNumber\":\"SN-001\",\"make\":\"Dell\",\"model\":\"XPS\",\"purchaseDate\":\"2024-01-01\"}";
        mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.type").exists());
    }

    @Test
    void create_withType_returns200() throws Exception {
        String payload = "{\"serialNumber\":\"SN-002\",\"make\":\"Dell\",\"model\":\"Latitude\",\"purchaseDate\":\"2024-01-01\",\"type\":\"LAPTOP\"}";

        Asset saved = new Asset();
        saved.setId(100L);
        saved.setAssetTag("AT-100");
        saved.setSerialNumber("SN-002");
        saved.setMake("Dell");
        saved.setModel("Latitude");
        saved.setPurchaseDate(java.time.LocalDate.parse("2024-01-01"));
        saved.setType(AssetType.LAPTOP);

        given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).willReturn(saved);

        mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.type").value("LAPTOP"));
    }
}
