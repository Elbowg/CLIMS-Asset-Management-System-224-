package com.clims.backend.security;

import com.clims.backend.dto.AuthRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;


    private String adminAccessToken;

    @BeforeEach
    void loginAdmin() throws Exception {
        AuthRequest req = new AuthRequest("admin", "admin");
    var result = mvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").exists())
        .andReturn();
        String body = result.getResponse().getContentAsString();
        var node = mapper.readTree(body);
        adminAccessToken = node.get("accessToken").asText();
    }

    @Test
    void unauthenticatedAssetListIs401() throws Exception {
    mvc.perform(get("/api/assets"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListAssets() throws Exception {
        mvc.perform(get("/api/assets")
                .header("Authorization", "Bearer " + adminAccessToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateAsset() throws Exception {
        String payload = "{\"name\":\"Laptop\",\"serialNumber\":\"SN-123\"}";
        mvc.perform(post("/api/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminAccessToken)
                .content(payload))
                .andExpect(status().isCreated());
    }
}
