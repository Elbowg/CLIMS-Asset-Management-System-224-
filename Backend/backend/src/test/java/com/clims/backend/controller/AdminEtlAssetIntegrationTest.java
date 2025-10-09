package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.summary.AssetStatusDaily;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.summary.AssetStatusDailyRepository;
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
class AdminEtlAssetIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired AssetRepository assetRepository;
    @Autowired AssetStatusDailyRepository summaryRepository;

    @BeforeEach
    void setup() {
        summaryRepository.deleteAll();
        assetRepository.deleteAll();
    }

    @Test
    void adminCanBackfillAssets_andIsIdempotent() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        Asset a1 = new Asset();
        a1.setName("Asset One");
        a1.setSerialNumber("SN-" + UUID.randomUUID());
        a1.setStatus(AssetStatus.AVAILABLE);
        a1.setPurchaseDate(yesterday);
        assetRepository.save(a1);

        Asset a2 = new Asset();
        a2.setName("Asset Two");
        a2.setSerialNumber("SN-" + UUID.randomUUID());
        a2.setStatus(AssetStatus.ASSIGNED);
        a2.setPurchaseDate(today);
        assetRepository.save(a2);

        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");

        // First run
        mockMvc.perform(post("/api/admin/etl/asset-status-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", yesterday.toString())
                        .param("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(yesterday, today);
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(r -> r.getId().getAssetStatus().name())
                .containsExactlyInAnyOrder("AVAILABLE", "ASSIGNED");

        // Second run (idempotent)
        mockMvc.perform(post("/api/admin/etl/asset-status-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", yesterday.toString())
                        .param("to", today.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        var rows2 = summaryRepository.findAllById_BucketDateBetweenOrderById_BucketDateAsc(yesterday, today);
        assertThat(rows2).hasSize(2);

        AssetStatusDaily y = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(yesterday))
                .findFirst().orElseThrow();
        assertThat(y.getAssetCount()).isEqualTo(1);

        AssetStatusDaily t = rows2.stream()
                .filter(r -> r.getId().getBucketDate().equals(today))
                .findFirst().orElseThrow();
        assertThat(t.getAssetCount()).isEqualTo(1);
    }

    @Test
    void rejectsTooLargeWindow() throws Exception {
        String token = TestAuthUtils.loginAndGetAccessToken(mockMvc, mapper, "admin", "admin");
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(500);
        mockMvc.perform(post("/api/admin/etl/asset-status-daily")
                        .header("Authorization", "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
