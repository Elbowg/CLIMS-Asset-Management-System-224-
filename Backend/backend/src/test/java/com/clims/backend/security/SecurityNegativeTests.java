package com.clims.backend.security;

import com.clims.backend.dto.AuthRequest;
import com.clims.backend.dto.RefreshRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.access-expiration=3000"  // 3 seconds for clearer expiry window
})
class SecurityNegativeTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private String userAccess;
    private String adminAccess;

    @BeforeEach
    void loginUsers() throws Exception {
        // login standard user
        var userLogin = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthRequest("user","user"))))
                .andExpect(status().isOk())
                .andReturn();
        var userNode = mapper.readTree(userLogin.getResponse().getContentAsString());
        userAccess = userNode.get("accessToken").asText();
        // refresh token retained only if needed in future scenarios

        // login admin for refresh misuse test (access token used as refresh)
        var adminLogin = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new AuthRequest("admin","admin"))))
                .andExpect(status().isOk())
                .andReturn();
        adminAccess = mapper.readTree(adminLogin.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
                void userCannotCreateAsset() throws Exception {
        String payload = "{\"name\":\"Printer\",\"serialNumber\":\"SN-USER-1\"}";
        mvc.perform(post("/api/assets")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization","Bearer " + userAccess)
                .content(payload))
                                        // Authenticated user lacks required privilege -> 403
                                        .andExpect(status().isForbidden());
    }

    @Test
    void refreshEndpointRejectsAccessToken() throws Exception {
        // Use admin access token in refresh endpoint
        RefreshRequest rr = new RefreshRequest();
        rr.setRefreshToken(adminAccess); // wrong token type
        mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(rr)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAccessTokenIsUnauthorized() throws Exception {
        // Wait >1s to ensure expiry
        Thread.sleep(3200); // exceed 3s TTL to guarantee expiry
        mvc.perform(get("/api/assets")
                .header("Authorization","Bearer " + userAccess))
                                // After introducing RestAuthenticationEntryPoint, expired token should produce 401
                                .andExpect(status().isUnauthorized());
    }

        @Test
        void authenticatedUserWithoutAdminRoleGetsForbidden() throws Exception {
                // Standard user tries an admin-only operation (assumes POST /api/assets requires admin)
                String payload = "{\"name\":\"Printer2\",\"serialNumber\":\"SN-USER-2\"}";
                mvc.perform(post("/api/assets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization","Bearer " + userAccess)
                                .content(payload))
                                .andExpect(status().isForbidden());
        }
}
