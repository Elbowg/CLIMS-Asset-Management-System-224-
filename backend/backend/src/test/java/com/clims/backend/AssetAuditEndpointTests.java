package com.clims.backend;

import com.clims.backend.controllers.AssetController;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.entities.AuditLog;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = AssetController.class)
@Import({GlobalExceptionHandler.class, com.clims.backend.security.SecurityConfig.class})
class AssetAuditEndpointTests {

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
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

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
    void audit_requiresAuth() throws Exception {
        mvc.perform(get("/api/assets/1/audit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void audit_assetExists_returnsList() throws Exception {
        given(assetService.get(1L)).willReturn(new Asset());
        AuditLog l1 = new AuditLog();
        l1.setId(10L);
        l1.setEntityName("Asset");
        l1.setEntityId(1L);
        l1.setAction("UPDATE");
        l1.setDetails("Changed status");
        given(auditLogService.findByEntity("Asset", 1L)).willReturn(List.of(l1));

        mvc.perform(get("/api/assets/1/audit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].entityName").value("Asset"))
                .andExpect(jsonPath("$[0].entityId").value(1))
                .andExpect(jsonPath("$[0].action").value("UPDATE"));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void audit_forbiddenForEmployee() throws Exception {
        mvc.perform(get("/api/assets/1/audit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void audit_assetNotFound_returns404() throws Exception {
        given(assetService.get(999L)).willThrow(new NotFoundException("Asset not found"));
        mvc.perform(get("/api/assets/999/audit").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
