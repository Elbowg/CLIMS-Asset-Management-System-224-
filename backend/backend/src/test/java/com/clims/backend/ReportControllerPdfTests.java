package com.clims.backend;

import com.clims.backend.controllers.ReportController;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.SecurityConfig;
import com.clims.backend.services.ReportService;
import org.hamcrest.Matchers;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = true)
public class ReportControllerPdfTests {

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
    void inventoryPdf_unfiltered_truncated_sets_headers_and_pdf_content_type() throws Exception {
        // Unfiltered request ({}), controller sets limit=5000 and adds truncation headers if service reports truncated
        ReportService.ReportBytes bytes = new ReportService.ReportBytes("%PDF-1.4".getBytes(), true);
        when(reportService.inventoryPdfLimited(any(), any())).thenReturn(bytes);

        mvc.perform(post("/api/reports/inventory/pdf").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment; filename=inventory_")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".pdf")))
                .andExpect(header().string("X-Report-Limited", "true"))
                .andExpect(header().string("X-Report-Limit", "5000"));
    }

    @Test
    @WithMockUser(roles = {"AUDITOR"})
    void maintenancePdf_allowed_role_has_pdf_filename_and_type_no_truncation_header() throws Exception {
        ReportService.ReportBytes bytes = new ReportService.ReportBytes("%PDF-1.4".getBytes(), false);
        when(reportService.maintenancePdfLimited(any(), any())).thenReturn(bytes);

        mvc.perform(post("/api/reports/maintenance/pdf").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment; filename=maintenance_")))
                .andExpect(header().string("Content-Disposition", Matchers.containsString(".pdf")))
                .andExpect(header().doesNotExist("X-Report-Limited"))
                .andExpect(header().doesNotExist("X-Report-Limit"));
    }
}
