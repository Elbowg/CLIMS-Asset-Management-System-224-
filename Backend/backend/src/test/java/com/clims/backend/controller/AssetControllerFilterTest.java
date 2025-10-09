package com.clims.backend.controller;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.UserRepository;
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
class AssetControllerFilterTest {

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired UserRepository userRepository;

    private Long assignedUserId;

    @BeforeEach
    void setup() {
        assetRepository.deleteAll();
        userRepository.deleteAll();
    User temp = new User();
    temp.setUsername("tester");
    temp.setPassword("x");
    temp.setEmail("tester@example.com");
    final User savedUser = userRepository.save(temp);
    assignedUserId = savedUser.getId();

        // Create assets with alternating status and one assigned to user
        IntStream.range(0, 30).forEach(i -> {
            Asset a = new Asset();
            a.setName("Asset-" + i);
            a.setSerialNumber("SNF" + i);
            a.setPurchaseDate(LocalDate.now());
            a.setStatus(i % 2 == 0 ? AssetStatus.AVAILABLE : AssetStatus.RETIRED);
            if (i == 5) {
                a.setAssignedUser(savedUser);
                a.setStatus(AssetStatus.ASSIGNED);
            }
            assetRepository.save(a);
        });
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void filterByStatus() throws Exception {
        mockMvc.perform(get("/api/assets?status=AVAILABLE").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void filterByAssignedUser() throws Exception {
        mockMvc.perform(get("/api/assets?assignedUserId=" + assignedUserId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].assignedUserId").value(assignedUserId));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void sortByIdDesc() throws Exception {
        mockMvc.perform(get("/api/assets?sort=id,desc&size=1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Asset-29"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void invalidSortField_fallsBackToIdAsc() throws Exception {
        // With size=1 the first element should be the earliest inserted (Asset-0) when falling back to id,asc
        mockMvc.perform(get("/api/assets?sort=unknownField,desc&size=1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Asset-0"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void invalidSortDirection_defaultsToAsc() throws Exception {
        // direction 'sideways' is invalid; should default to asc; first element remains Asset-0
        mockMvc.perform(get("/api/assets?sort=name,sideways&size=1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Asset-0"));
    }
}
