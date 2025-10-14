package com.clims.backend;

import com.clims.backend.controllers.MaintenanceController;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.services.MaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import com.clims.backend.models.enums.MaintenanceStatus;
import java.time.LocalDate;

import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = MaintenanceController.class)
@Import(GlobalExceptionHandler.class)
public class MaintenanceControllerSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    MaintenanceService maintenanceService;

    // Security mocks
    @MockBean
    JwtAuthFilter jwtAuthFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CurrentUserService currentUserService;

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
        mvc.perform(get("/api/maintenance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withAdmin_ok() throws Exception {
    given(maintenanceService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0,10), 0));
        mvc.perform(get("/api/maintenance").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withPaging_structure_ok() throws Exception {
    Maintenance m1 = new Maintenance(); m1.setId(1L);
    Maintenance m2 = new Maintenance(); m2.setId(2L);
    given(maintenanceService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(java.util.List.of(m1,m2), PageRequest.of(1,2), 4));

    mvc.perform(get("/api/maintenance").param("page","1").param("size","2").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content").isArray())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(4))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalPages").value(2))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.page").value(1))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.size").value(2));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_withParams_mapsPageableAndFilters() throws Exception {
    given(maintenanceService.search(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .willReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(3,5, Sort.by("scheduledDate").descending()), 0));

    mvc.perform(get("/api/maintenance")
            .param("page","3")
            .param("size","5")
            .param("sort","scheduledDate,desc")
            .param("status","COMPLETED")
            .param("assetId","100")
            .param("dateFrom", LocalDate.of(2025,1,1).toString())
            .param("dateTo", LocalDate.of(2025,12,31).toString())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor = ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
    ArgumentCaptor<MaintenanceStatus> statusCaptor = ArgumentCaptor.forClass(MaintenanceStatus.class);
    ArgumentCaptor<Long> assetCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<LocalDate> fromCaptor = ArgumentCaptor.forClass(LocalDate.class);
    ArgumentCaptor<LocalDate> toCaptor = ArgumentCaptor.forClass(LocalDate.class);

    verify(maintenanceService).search(pageableCaptor.capture(), statusCaptor.capture(), assetCaptor.capture(), fromCaptor.capture(), toCaptor.capture());

    var pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(3);
    assertThat(pageable.getPageSize()).isEqualTo(5);
    assertThat(pageable.getSort().getOrderFor("scheduledDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    assertThat(statusCaptor.getValue()).isEqualTo(MaintenanceStatus.COMPLETED);
    assertThat(assetCaptor.getValue()).isEqualTo(100L);
    assertThat(fromCaptor.getValue()).isEqualTo(LocalDate.of(2025,1,1));
    assertThat(toCaptor.getValue()).isEqualTo(LocalDate.of(2025,12,31));
    }

    @Test
    @WithMockUser(roles = {"IT_STAFF"})
    void schedule_withItStaff_ok() throws Exception {
        String body = "{\"assetId\":1,\"description\":\"Check\"}";
        Maintenance m = new Maintenance();
        m.setId(1L);
        given(maintenanceService.schedule(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(m);
        AppUser actor = new AppUser();
        actor.setUsername("it_staff_user");
        given(currentUserService.requireCurrentUser()).willReturn(actor);
    mvc.perform(post("/api/maintenance").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<AppUser> actorCaptor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(maintenanceService).schedule(org.mockito.ArgumentMatchers.any(), actorCaptor.capture());
        assertThat(actorCaptor.getValue().getUsername()).isEqualTo("it_staff_user");
    }

    @Test
    @WithMockUser(roles = {"TECHNICIAN"})
    void updateStatus_withTech_ok() throws Exception {
        String body = "{\"status\":\"COMPLETED\"}";
        Maintenance m = new Maintenance();
        m.setId(1L);
        given(maintenanceService.updateStatus(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(m);
        AppUser actor = new AppUser();
        actor.setUsername("tech_user");
        given(currentUserService.requireCurrentUser()).willReturn(actor);
    mvc.perform(patch("/api/maintenance/1/status").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<AppUser> actorCaptor = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(maintenanceService).updateStatus(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(), actorCaptor.capture());
        assertThat(actorCaptor.getValue().getUsername()).isEqualTo("tech_user");
    }

    @Test
    void getById_requiresAuth() throws Exception {
        mvc.perform(get("/api/maintenance/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"TECHNICIAN"})
    void getById_withAllowedRole_ok() throws Exception {
        Maintenance m = new Maintenance();
        m.setId(1L);
        given(maintenanceService.get(1L)).willReturn(m);
        mvc.perform(get("/api/maintenance/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"TECHNICIAN"})
    void getById_notFound_returns404() throws Exception {
        given(maintenanceService.get(42L)).willThrow(new NotFoundException("Maintenance not found"));
        mvc.perform(get("/api/maintenance/42").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
