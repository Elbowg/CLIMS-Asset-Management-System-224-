package com.clims.backend.controller;

import com.clims.backend.dto.AuthRequest;
import com.clims.backend.dto.RefreshRequest;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.security.JwtKeyProvider;
import com.clims.backend.config.JwtProperties;
import com.clims.backend.security.TokenBlacklist;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    MockMvc mvc;
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    TokenBlacklist tokenBlacklist;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        authenticationManager = mock(AuthenticationManager.class);
        tokenBlacklist = mock(TokenBlacklist.class);
        JwtProperties props = new JwtProperties();
        props.setSecret("01234567890123456789012345678901");
        props.setAccessExpiration(60000L);
        props.setRefreshExpiration(120000L);
        JwtKeyProvider keyProvider = new JwtKeyProvider();
        keyProvider.setSecret(props.getSecret());
        jwtUtil = new JwtUtil(props, keyProvider);

        // AuditService not relevant for these tests; pass a simple mock
        com.clims.backend.service.AuditService auditService = mock(com.clims.backend.service.AuditService.class);
        AuthController controller = new AuthController(authenticationManager, jwtUtil, tokenBlacklist, null, auditService);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void loginReturnsTokens() throws Exception {
        User principal = new User("alice", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        AuthRequest req = new AuthRequest("alice", "pass");
        mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshReturnsNewTokens() throws Exception {
        // first produce a refresh token
        User principal = new User("bob", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        String rt = jwtUtil.generateRefreshToken("bob");
        RefreshRequest rr = new RefreshRequest(); rr.setRefreshToken(rt);
    when(tokenBlacklist.isBlacklisted(anyString())).thenReturn(false);

        mvc.perform(post("/api/auth/refresh").contentType("application/json").content(mapper.writeValueAsString(rr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }
}
