package com.clims.backend.controller;

import com.clims.backend.config.JwtProperties;
import com.clims.backend.dto.AuthRequest;
import com.clims.backend.dto.RefreshRequest;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.security.JwtKeyProvider;
import com.clims.backend.security.TokenBlacklist;
import com.clims.backend.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Focused controller-level test verifying refresh token blacklisting behavior:
 * - A legitimate refresh succeeds once
 * - Reusing the old (now blacklisted) refresh token is rejected (401)
 * - Logout blacklists a valid refresh token preventing future use
 */
class AuthControllerBlacklistIntegrationTest {
    MockMvc mvc;
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    TokenBlacklist tokenBlacklist;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        authenticationManager = mock(AuthenticationManager.class);
        tokenBlacklist = spy(new TokenBlacklistService());
        JwtProperties props = new JwtProperties();
        props.setSecret("01234567890123456789012345678901");
        props.setAccessExpiration(60000L);
        props.setRefreshExpiration(120000L);
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.setSecret(props.getSecret());
        jwtUtil = new JwtUtil(props, keyProvider);

        com.clims.backend.service.AuditService auditService = mock(com.clims.backend.service.AuditService.class);
        AuthController controller = new AuthController(authenticationManager, jwtUtil, tokenBlacklist, null, auditService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private Authentication authFor(String user) {
        User principal = new User(user, "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
    }

    @Test
    void refreshCannotBeReused() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(authFor("alice"));
        // First perform login to match flow (not strictly required for refresh token generation here)
        AuthRequest req = new AuthRequest("alice", "pass");
        mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        String originalRefresh = jwtUtil.generateRefreshToken("alice");
        RefreshRequest refreshReq = new RefreshRequest(); refreshReq.setRefreshToken(originalRefresh);

        // First refresh should be OK
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // Second attempt using same (now blacklisted) token should 401
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutBlacklistsRefreshToken() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(authFor("bob"));
        String refresh = jwtUtil.generateRefreshToken("bob");

        // logout should 204
        RefreshRequest rr = new RefreshRequest(); rr.setRefreshToken(refresh);
        mvc.perform(post("/api/auth/logout").contentType("application/json").content(mapper.writeValueAsString(rr)))
                .andExpect(status().isNoContent());

        // Attempt to use logged-out refresh should 401 (invalid because blacklisted)
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(rr)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blacklistedRefreshReturnsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(authFor("carol"));
        String refresh = jwtUtil.generateRefreshToken("carol");

        // Use refresh once to cause rotation + blacklist original
        RefreshRequest first = new RefreshRequest(); first.setRefreshToken(refresh);
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        // Second attempt with same token should 401 (payload format verified in GlobalExceptionHandlerTest elsewhere)
        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(first)))
                .andExpect(status().isUnauthorized());
    }
}
