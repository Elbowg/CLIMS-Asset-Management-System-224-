package com.clims.backend;

import com.clims.backend.controllers.ReportController;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.SecurityConfig;
import com.clims.backend.services.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.hamcrest.Matchers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = ReportController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = true)
public class ReportControllerSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @MockBean
    ReportService reportService;

    @MockBean
    org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        Mockito.doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void inventoryCsv_admin_allowed() throws Exception {
        given(reportService.countInventoryRecords(any())).willReturn(1L);
        doAnswer(inv -> {
            java.io.OutputStream os = inv.getArgument(0);
            os.write("id,name\n1,Laptop\n".getBytes());
            return null;
        }).when(reportService).writeInventoryCsv(any(), any(), any());

        MediaType csv = new MediaType("text", "csv");

        mvc.perform(post("/api/reports/inventory/csv").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(csv))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", Matchers.containsString("attachment; filename=inventory_")))
                .andExpect(MockMvcResultMatchers.header().string("Content-Disposition", Matchers.containsString(".csv")));
    }

    @Test
    @WithMockUser(roles = {"EMPLOYEE"})
    void inventoryCsv_employee_forbidden() throws Exception {
    mvc.perform(post("/api/reports/inventory/csv").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inventoryCsv_unauthenticated_401() throws Exception {
    mvc.perform(post("/api/reports/inventory/csv").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
