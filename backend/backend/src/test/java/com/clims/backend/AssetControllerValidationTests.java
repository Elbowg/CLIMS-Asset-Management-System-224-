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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AssetController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = {"ADMIN"})
class AssetControllerValidationTests {

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
    void create_duplicateSerial_returns409() throws Exception {
        // Simulate service throwing when duplicate serial detected
        given(assetService.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willThrow(new IllegalStateException("Serial number already exists"));

        String payload = "{\"serialNumber\":\"DUP-SN\",\"make\":\"Dell\",\"model\":\"X\",\"purchaseDate\":\"2024-01-01\"}";
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Serial number already exists"));
    }

    @Test
    void create_missingSerial_returns400() throws Exception {
        // Missing serialNumber should trigger validation error
        String payload = "{\"make\":\"Dell\",\"model\":\"X\",\"purchaseDate\":\"2024-01-01\"}";
    mvc.perform(post("/api/assets").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.serialNumber").exists());
    }
}
