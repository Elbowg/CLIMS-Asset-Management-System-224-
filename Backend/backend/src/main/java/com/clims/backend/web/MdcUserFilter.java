package com.clims.backend.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates MDC with authenticated username for request-scoped structured logging.
 * Key: "user". Removed at the end of the request to avoid thread-local leakage.
 */
@Component
public class MdcUserFilter extends OncePerRequestFilter {
    public static final String USER_MDC_KEY = "user";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        boolean populated = false;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                String username = authentication.getName();
                if (username != null && !username.isBlank()) {
                    MDC.put(USER_MDC_KEY, username);
                    populated = true;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (populated) {
                MDC.remove(USER_MDC_KEY);
            }
        }
    }
}
