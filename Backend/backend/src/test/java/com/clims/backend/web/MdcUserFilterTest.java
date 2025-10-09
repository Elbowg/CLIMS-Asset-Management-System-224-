package com.clims.backend.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MdcUserFilterTest {

    private final MdcUserFilter filter = new MdcUserFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void addsUsernameToMdcForAuthenticatedUser() throws ServletException, IOException {
        // given an authenticated user
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "alice", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] userSeenInChain = {null};

        FilterChain chain = (request1, response1) -> userSeenInChain[0] = MDC.get(MdcUserFilter.USER_MDC_KEY);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(userSeenInChain[0]).isEqualTo("alice");
        assertThat(MDC.get(MdcUserFilter.USER_MDC_KEY)).as("MDC should be cleared after filter").isNull();
    }

    @Test
    void doesNotPopulateForAnonymous() throws ServletException, IOException {
        // given an anonymous auth present
        AnonymousAuthenticationToken anon = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anon);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        final String[] userSeenInChain = {"marker"};

        FilterChain chain = (request1, response1) -> userSeenInChain[0] = MDC.get(MdcUserFilter.USER_MDC_KEY);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(userSeenInChain[0]).isNull();
        assertThat(MDC.get(MdcUserFilter.USER_MDC_KEY)).isNull();
    }
}
