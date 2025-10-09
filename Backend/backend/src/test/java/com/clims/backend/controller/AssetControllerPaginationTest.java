package com.clims.backend.controller;

import com.clims.backend.repository.AssetRepository;
import com.clims.backend.model.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerPaginationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    AssetRepository assetRepository;

    @BeforeEach
    void setup() {
        assetRepository.deleteAll();
        IntStream.range(0, 150).forEach(i -> {
            Asset a = new Asset();
            a.setName("Asset-" + i);
            a.setSerialNumber("SN" + i);
            a.setPurchaseDate(LocalDate.now());
            assetRepository.save(a);
        });
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void defaultPageSizeIs20() throws Exception {
        mockMvc.perform(get("/api/assets").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(150));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void maxPageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(get("/api/assets?size=500").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content.length()").value(100))
                .andExpect(jsonPath("$.totalElements").value(150));
    }
}
