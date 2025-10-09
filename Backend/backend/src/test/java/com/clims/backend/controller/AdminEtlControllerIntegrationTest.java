package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.Maintenance;
import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.model.summary.MaintenanceActivityDaily;
import com.clims.backend.model.summary.MaintenanceActivityDailyId;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.repository.summary.MaintenanceActivityDailyRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminEtlControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired MaintenanceRepository maintenanceRepository;
    @Autowired AssetRepository assetRepository;
    @Autowired MaintenanceActivityDailyRepository summaryRepository;

    @BeforeEach
    void setup() {
        maintenanceRepository.deleteAll();
        assetRepository.deleteAll();
        summaryRepository.deleteAll();
    }

    @Test
    void adminCanBackfill_andIsIdempotent() throws Exception {
        Asset asset = new Asset();
        asset.setName("ETL Asset");
        asset.setSerialNumber("SN-" + UUID.randomUUID());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset = assetRepository.save(asset);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Maintenance m1 = new Maintenance();
        m1.setAsset(asset);
        m1.setStatus(MaintenanceStatus.REPORTED);
        m1.setReportedAt(yesterday.atTime(9, 0));
        maintenanceRepository.save(m1);

        Maintenance m2 = new Maintenance();
        m2.setAsset(asset);
        m2.setStatus(MaintenanceStatus.RESOLVED);
        m2.setReportedAt(today.atTime(10, 0));
        maintenanceRepository.save(m2);

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        // First run
        mockMvc.perform(post("/api/admin/etl/maintenance-activity-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", yesterday.toString())
                        .param("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(yesterday, today);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.getId().getMaintenanceStatus().name())
                .containsExactlyInAnyOrder("REPORTED", "RESOLVED");

        // Second run (idempotent)
        mockMvc.perform(post("/api/admin/etl/maintenance-activity-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", yesterday.toString())
                        .param("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows2 = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(yesterday, today);
        assertThat(rows2).hasSize(2);

        // Verify counts
        MaintenanceActivityDaily y = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(yesterday))
                .findFirst().orElseThrow();
        assertThat(y.getActivityCount()).isEqualTo(1);

        MaintenanceActivityDaily t = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(today))
                .findFirst().orElseThrow();
        assertThat(t.getActivityCount()).isEqualTo(1);
    }

    @Test
    void rejectsTooLargeWindow() throws Exception {
        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(500);
        mockMvc.perform(post("/api/admin/etl/maintenance-activity-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
