package com.clims.backend;

import com.clims.backend.controllers.AuthController;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
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
import org.springframework.security.authentication.AuthenticationManager;
import com.clims.backend.services.UserService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
public class AuthControllerSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtAuthFilter jwtAuthFilter;
    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    CurrentUserService currentUserService;

    @MockBean
    AuthenticationManager authenticationManager;

    @MockBean
    UserService userService;

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
    void me_unauthenticated_returns401() throws Exception {
        given(currentUserService.requireCurrentUser()).willReturn(null);
        mvc.perform(get("/api/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void me_authenticated_returnsUserResponse() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        given(currentUserService.requireCurrentUser()).willReturn(user);

        mvc.perform(get("/api/auth/me").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }
}
