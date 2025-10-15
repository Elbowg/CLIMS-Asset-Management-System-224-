package com.clims.backend;

import com.clims.backend.controllers.AuthController;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.RefreshToken;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.services.RefreshTokenService;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.clims.backend.exceptions.GlobalExceptionHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class AuthControllerChangeAndRefreshTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtAuthFilter jwtAuthFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CurrentUserService currentUserService;
        @MockBean
        org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @MockBean
    UserService userService;
    @MockBean
    RefreshTokenService refreshTokenService;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        Mockito.doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        given(currentUserService.requireCurrentUser()).willReturn(null);

        String body = "{" +
                "\"currentPassword\":\"x\"," +
                "\"newPassword\":\"y\"" +
                "}";

        mvc.perform(post("/api/auth/change-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_success_returns200() throws Exception {
        AppUser user = new AppUser();
        user.setId(5L);
        given(currentUserService.requireCurrentUser()).willReturn(user);

        // stub refresh token creation to return a token so controller returns it
        RefreshToken created = new RefreshToken();
        created.setToken("rotated");
        created.setUser(user);
        given(refreshTokenService.createRefreshToken(user)).willReturn(created);

        String body = "{" +
                "\"currentPassword\":\"old\"," +
                "\"newPassword\":\"new\"" +
                "}";

        mvc.perform(post("/api/auth/change-password").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Password changed"))
        .andExpect(jsonPath("$.refreshToken").isString());
    }

    @Test
    void changePassword_weakPassword_returns400() throws Exception {
    AppUser user = new AppUser();
    user.setId(6L);
    given(currentUserService.requireCurrentUser()).willReturn(user);

    // simulate service rejecting weak password
    Mockito.doThrow(new IllegalArgumentException("Password must be at least 8 characters"))
        .when(userService).changePassword(user.getId(), "old", "weak");

    String body = "{" +
        "\"currentPassword\":\"old\"," +
        "\"newPassword\":\"weak\"" +
        "}";

    mvc.perform(post("/api/auth/change-password").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_wrongCurrent_returns400() throws Exception {
    AppUser user = new AppUser();
    user.setId(7L);
    given(currentUserService.requireCurrentUser()).willReturn(user);

    // simulate wrong current password
    Mockito.doThrow(new IllegalArgumentException("Current password is incorrect"))
        .when(userService).changePassword(user.getId(), "old", "new");

    String body = "{" +
        "\"currentPassword\":\"old\"," +
        "\"newPassword\":\"new\"" +
        "}";

    mvc.perform(post("/api/auth/change-password").with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_withInvalidToken_returns401() throws Exception {
        given(refreshTokenService.findByToken("bad")).willReturn(Optional.empty());

        String body = "{" +
                "\"refreshToken\":\"bad\"" +
                "}";

        mvc.perform(post("/api/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_withValidToken_returnsToken() throws Exception {
        AppUser user = new AppUser();
        user.setUsername("alice");
        RefreshToken rt = new RefreshToken();
        rt.setToken("good");
        rt.setUser(user);
        given(refreshTokenService.findByToken("good")).willReturn(Optional.of(rt));
    given(jwtUtil.generateToken(Mockito.eq("alice"), Mockito.any())).willReturn("newtoken");

        String body = "{" +
                "\"refreshToken\":\"good\"" +
                "}";

        mvc.perform(post("/api/auth/refresh").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("newtoken"));
    }
}
