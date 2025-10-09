package com.clims.backend.controller;

import com.clims.backend.model.AuditEvent;
import com.clims.backend.model.summary.AuditActionDaily;
import com.clims.backend.repository.AuditEventRepository;
import com.clims.backend.repository.summary.AuditActionDailyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEtlAuditIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuditEventRepository auditEventRepository;
    @Autowired AuditActionDailyRepository summaryRepository;

    @BeforeEach
    void setup() {
        summaryRepository.deleteAll();
        auditEventRepository.deleteAll();
    }

    @Test
    void adminCanBackfillAudit_andIsIdempotent() throws Exception {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        LocalDate twoDaysAgo = today.minusDays(2);

        // Two events across two days with different actions
                // Note: We intentionally avoid including 'today' in the ETL window to exclude
                // the login-generated audit event created by the test's authentication call.
                // This keeps the expected row count deterministic for this test case.
        AuditEvent e1 = new AuditEvent(twoDaysAgo.atStartOfDay().plusHours(8).toInstant(ZoneOffset.UTC), "alice", "LOGIN", null, "127.0.0.1", "req-1");
        auditEventRepository.save(e1);
        AuditEvent e2 = new AuditEvent(yesterday.atStartOfDay().plusHours(10).toInstant(ZoneOffset.UTC), "bob", "LOGOUT", null, "127.0.0.1", "req-2");
        auditEventRepository.save(e2);

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        // First run
        mockMvc.perform(post("/api/admin/etl/audit-action-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", twoDaysAgo.toString())
                        .param("to", yesterday.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(twoDaysAgo, yesterday);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.getId().getAuditAction())
                .containsExactlyInAnyOrder("LOGIN", "LOGOUT");

        // Second run (idempotent)
        mockMvc.perform(post("/api/admin/etl/audit-action-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", twoDaysAgo.toString())
                        .param("to", yesterday.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows2 = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(twoDaysAgo, yesterday);
        assertThat(rows2).hasSize(2);

        AuditActionDaily y = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(twoDaysAgo))
                .findFirst().orElseThrow();
        assertThat(y.getActionCount()).isEqualTo(1);

        AuditActionDaily t = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(yesterday))
                .findFirst().orElseThrow();
        assertThat(t.getActionCount()).isEqualTo(1);
    }

    @Test
    void rejectsTooLargeWindow() throws Exception {
        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");
        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(500);
        mockMvc.perform(post("/api/admin/etl/audit-action-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
