package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.summary.AssetStatusDaily;
import com.clims.backend.model.summary.AssetStatusDailyId;
import com.clims.backend.model.summary.AuditActionDaily;
import com.clims.backend.model.summary.AuditActionDailyId;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.summary.AssetStatusDailyRepository;
import com.clims.backend.repository.summary.AuditActionDailyRepository;
import com.clims.backend.repository.MaintenanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsSummaryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired AssetRepository assetRepository;
    @Autowired AssetStatusDailyRepository assetStatusDailyRepository;
    @Autowired AuditActionDailyRepository auditActionDailyRepository;
    @Autowired MaintenanceRepository maintenanceRepository;

    @BeforeEach
    void clean() {
        auditActionDailyRepository.deleteAll();
        assetStatusDailyRepository.deleteAll();
        // Delete maintenance first to satisfy FK_MAINTENANCE_ASSET
        maintenanceRepository.deleteAll();
        assetRepository.deleteAll();
    }

    @Test
    void assetsDailyStatus_requiresRole() throws Exception {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);
        mockMvc.perform(get("/api/reports/assets/daily-status")
                .param("from", from.toString())
                .param("to", to.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void assetsDailyStatus_returnsSummaryRows() throws Exception {
        // Seed an asset and two summary rows across two days
        Asset asset = new Asset();
        asset.setName("A-" + UUID.randomUUID());
        asset.setSerialNumber("SN-" + UUID.randomUUID());
        asset.setStatus(AssetStatus.ASSIGNED);
        assetRepository.save(asset);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        assetStatusDailyRepository.save(new AssetStatusDaily(new AssetStatusDailyId(yesterday, AssetStatus.ASSIGNED), 1));
        assetStatusDailyRepository.save(new AssetStatusDaily(new AssetStatusDailyId(today, AssetStatus.AVAILABLE), 2));

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        var mvcResult = mockMvc.perform(get("/api/reports/assets/daily-status")
                .header("Authorization", "Bearer " + token)
                .param("from", yesterday.toString())
                .param("to", today.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode arr = mapper.readTree(mvcResult.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void auditDailyActions_returnsSummaryRows() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        auditActionDailyRepository.save(new AuditActionDaily(new AuditActionDailyId(yesterday, "LOGIN"), 1));
        auditActionDailyRepository.save(new AuditActionDaily(new AuditActionDailyId(today, "REFRESH"), 2));

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        var mvcResult = mockMvc.perform(get("/api/reports/audit/daily-actions")
                .header("Authorization", "Bearer " + token)
                .param("from", yesterday.toString())
                .param("to", today.toString())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode arr = mapper.readTree(mvcResult.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);
    }
}
