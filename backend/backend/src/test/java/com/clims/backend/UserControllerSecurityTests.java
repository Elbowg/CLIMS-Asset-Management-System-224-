package com.clims.backend;

import com.clims.backend.controllers.UserController;
import com.clims.backend.dto.UserDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.services.UserService;
import com.clims.backend.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import com.clims.backend.exceptions.GlobalExceptionHandler;
import com.clims.backend.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@WebMvcTest(controllers = UserController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
public class UserControllerSecurityTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    UserService userService;

    // Security/infra mocks
    @MockBean
    JwtAuthFilter jwtAuthFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    ModelMapper modelMapper;
    @MockBean
    UserDetailsService userDetailsService;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void register_requiresAdmin() throws Exception {
        String body = "{\"username\":\"a\",\"email\":\"a@b.com\",\"password\":\"p\"}";
    mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void register_withAdmin_ok() throws Exception {
        given(userService.register(any(UserDtos.RegisterRequest.class))).willAnswer(inv -> {
            AppUser u = new AppUser();
            u.setId(11L);
            u.setUsername("a");
            u.setEmail("a@b.com");
            u.setRole(com.clims.backend.security.Role.ADMIN);
            return u;
        });
        String body = "{\"username\":\"a\",\"email\":\"a@b.com\",\"password\":\"p\"}";
    mvc.perform(post("/api/users").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void getById_requiresAuth() throws Exception {
        mvc.perform(get("/api/users/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"IT_STAFF"})
    void getById_forbiddenForNonAdmin() throws Exception {
        mvc.perform(get("/api/users/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getById_withAdmin_ok() throws Exception {
        AppUser u = new AppUser();
        u.setId(1L);
        u.setUsername("admin");
        u.setEmail("admin@ex.com");
        u.setRole(com.clims.backend.security.Role.ADMIN);
        given(userService.get(1L)).willReturn(u);
        mvc.perform(get("/api/users/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void getById_notFound_returns404() throws Exception {
        given(userService.get(77L)).willThrow(new NotFoundException("User not found"));
        mvc.perform(get("/api/users/77").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
