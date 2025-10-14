package com.clims.backend;

import com.clims.backend.controllers.UserController;
import com.clims.backend.dto.UserDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.security.JwtAuthFilter;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.security.Role;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({com.clims.backend.exceptions.GlobalExceptionHandler.class, com.clims.backend.security.SecurityConfig.class})
class AdminUserManagementControllerTests {

    @Autowired
    MockMvc mvc;

    @MockBean
    UserService userService;

    @MockBean
    JwtAuthFilter jwtAuthFilter;

    @MockBean
    JwtUtil jwtUtil;

    @MockBean
    UserDetailsService userDetailsService;

    @MockBean
    ModelMapper modelMapper;

    @BeforeEach
    void setupFilterChainPassThrough() throws Exception {
        org.mockito.Mockito.doAnswer(invocation -> {
            jakarta.servlet.ServletRequest req = invocation.getArgument(0);
            jakarta.servlet.ServletResponse res = invocation.getArgument(1);
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthFilter).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void list_requiresAuth() throws Exception {
        mvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"IT_STAFF"})
    void list_forbiddenForNonAdmin() throws Exception {
        mvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void list_ok() throws Exception {
        AppUser u = new AppUser(); u.setId(1L); u.setUsername("admin"); u.setEmail("admin@ex.com"); u.setRole(Role.ADMIN);
        Page<AppUser> page = new PageImpl<>(Collections.singletonList(u), PageRequest.of(0,10), 1);
        given(userService.search(any(), any(), any(), any())).willReturn(page);
        mvc.perform(get("/api/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void patchRole_requiresAuth() throws Exception {
        mvc.perform(patch("/api/users/1/role").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"IT_STAFF"})
    void patchRole_forbiddenForNonAdmin() throws Exception {
        mvc.perform(patch("/api/users/1/role").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void patchRole_ok() throws Exception {
        AppUser u = new AppUser(); u.setId(1L); u.setUsername("admin"); u.setEmail("admin@ex.com"); u.setRole(Role.MANAGER);
        given(userService.updateRole(1L, Role.MANAGER)).willReturn(u);
        mvc.perform(patch("/api/users/1/role").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void patchDepartment_ok() throws Exception {
        AppUser u = new AppUser(); u.setId(1L); u.setUsername("admin"); u.setEmail("admin@ex.com"); u.setRole(Role.ADMIN);
        given(userService.updateDepartment(1L, 77L)).willReturn(u);
        mvc.perform(patch("/api/users/1/department").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"departmentId\":77}"))
                .andExpect(status().isOk());
    }
}
