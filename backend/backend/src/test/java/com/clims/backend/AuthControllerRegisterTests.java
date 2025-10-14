package com.clims.backend;

import com.clims.backend.controllers.AuthController;
import com.clims.backend.dto.UserDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import com.clims.backend.exceptions.GlobalExceptionHandler;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = AuthController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class AuthControllerRegisterTests {

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
    void register_success_returns201() throws Exception {
        AppUser created = new AppUser();
        created.setId(42L);
        created.setUsername("bob");
        created.setEmail("bob@example.com");
        given(userService.register(any(UserDtos.RegisterRequest.class))).willReturn(created);

        String body = "{" +
                "\"username\":\"bob\"," +
                "\"email\":\"bob@example.com\"," +
                "\"password\":\"S3cret!\"" +
                "}";

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void register_duplicate_returns409() throws Exception {
        given(userService.register(any(UserDtos.RegisterRequest.class)))
                .willThrow(new IllegalStateException("Username already exists"));

        String body = "{" +
                "\"username\":\"alice\"," +
                "\"email\":\"alice@example.com\"," +
                "\"password\":\"pass\"" +
                "}";

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already exists"));
    }

    @Test
    void register_validationErrors_returns400() throws Exception {
        // Missing username and invalid email
        String body = "{" +
                "\"email\":\"not-an-email\"," +
                "\"password\":\"p\"" +
                "}";

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists());
    }
}
