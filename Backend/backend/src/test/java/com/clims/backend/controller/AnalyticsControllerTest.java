package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.Maintenance;
import com.clims.backend.model.MaintenanceStatus;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.MaintenanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired MaintenanceRepository maintenanceRepository;
    @Autowired AssetRepository assetRepository;

    @Test
    void requiresRoleForAccess() throws Exception {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(7);
        mockMvc.perform(get("/api/reports/maintenance/workload")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTooLargeWindow() throws Exception {
        // login as admin
        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(400);
        mockMvc.perform(get("/api/reports/maintenance/workload")
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsEmptyArrayWhenNoData() throws Exception {
        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(2);
        var mvcResult = mockMvc.perform(get("/api/reports/maintenance/workload")
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode arr = mapper.readTree(mvcResult.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
    }

    @Test
    void returnsAggregatedCountsWithData() throws Exception {
        // Arrange: seed minimal Asset and Maintenance rows on two days
        maintenanceRepository.deleteAll();
        assetRepository.deleteAll();

        Asset asset = new Asset();
        asset.setName("Test Asset");
        asset.setSerialNumber("SN-" + UUID.randomUUID());
        asset.setStatus(AssetStatus.AVAILABLE);
        asset = assetRepository.save(asset);

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Maintenance m1 = new Maintenance();
        m1.setAsset(asset);
        m1.setStatus(MaintenanceStatus.REPORTED);
        m1.setReportedAt(yesterday.atTime(10, 0));
        maintenanceRepository.save(m1);

        Maintenance m2 = new Maintenance();
        m2.setAsset(asset);
        m2.setStatus(MaintenanceStatus.REPORTED);
        m2.setReportedAt(today.atTime(11, 0));
        maintenanceRepository.save(m2);

        Maintenance m3 = new Maintenance();
        m3.setAsset(asset);
        m3.setStatus(MaintenanceStatus.RESOLVED);
        m3.setReportedAt(today.atTime(12, 0));
        maintenanceRepository.save(m3);

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        var mvcResult = mockMvc.perform(get("/api/reports/maintenance/workload")
                        .header("Authorization", "Bearer " + token)
                        .param("from", yesterday.toString())
                        .param("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = mapper.readTree(mvcResult.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        // Expect at least 3 grouped entries across the two days (REPORTED x2, RESOLVED x1)
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);

        // Verify specific groups exist with expected counts
        boolean foundYesterdayReported = false;
        boolean foundTodayReported = false;
        boolean foundTodayResolved = false;

        for (Iterator<JsonNode> it = arr.elements(); it.hasNext(); ) {
            JsonNode node = it.next();
            LocalDate day = LocalDate.parse(node.get("day").asText());
            String status = node.get("status").asText();
            long count = node.get("count").asLong();

            if (day.equals(yesterday) && status.equals("REPORTED") && count == 1) {
                foundYesterdayReported = true;
            }
            if (day.equals(today) && status.equals("REPORTED") && count == 1) {
                foundTodayReported = true;
            }
            if (day.equals(today) && status.equals("RESOLVED") && count == 1) {
                foundTodayResolved = true;
            }
        }

        assertThat(foundYesterdayReported).isTrue();
        assertThat(foundTodayReported).isTrue();
        assertThat(foundTodayResolved).isTrue();
    }
}
