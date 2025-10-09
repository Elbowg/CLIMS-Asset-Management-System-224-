package com.clims.backend.controller;

import com.clims.backend.dto.AuthRequest;
import com.clims.backend.dto.TokenPairResponse;
import com.clims.backend.dto.RefreshRequest;
import com.clims.backend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.clims.backend.security.TokenBlacklist;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.clims.backend.exception.ErrorResponse;
import com.clims.backend.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist; // interface-based for pluggable implementations
    private final MeterRegistry meterRegistry;
    private final AuditService auditService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          TokenBlacklist tokenBlacklist,
                          MeterRegistry meterRegistry,
                          AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.meterRegistry = meterRegistry;
        this.auditService = auditService;
    }

    private Timer timer(String name) {
        if (meterRegistry == null) {
            return Timer.builder(name).register(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        }
        return meterRegistry.timer(name);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and obtain JWT token pair", description = "Authenticates credentials and returns access & refresh tokens with expirations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Authenticated; tokens issued"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenPairResponse> authenticateUser(@RequestBody AuthRequest loginRequest, HttpServletRequest request) {
    return timer("auth.login.timer").record(() -> {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String access = jwtUtil.generateAccessToken(authentication);
        String refresh = jwtUtil.generateRefreshToken(loginRequest.getUsername());
        if (meterRegistry != null) {
            meterRegistry.counter("auth.login.success").increment();
            meterRegistry.counter("auth.access.issued", "flow", "login").increment();
            meterRegistry.counter("auth.refresh.issued", "flow", "login").increment();
        }
        auditService.record(loginRequest.getUsername(), "LOGIN", null, request.getRemoteAddr(), request.getHeader("X-Request-Id"));
        return ResponseEntity.ok(new TokenPairResponse(access, refresh, jwtUtil.getAccessExpirationSeconds(), jwtUtil.getRefreshExpirationSeconds()));
    });
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh & issue new access token", description = "Validates an existing (non-blacklisted) refresh token, rotates it, blacklists old jti, and returns new pair.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Refresh successful"),
        @ApiResponse(responseCode = "401", description = "Invalid or blacklisted refresh token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        String rt = request.getRefreshToken();
        if (rt == null || !jwtUtil.validateJwtToken(rt) || !jwtUtil.isRefreshToken(rt)) {
            if (meterRegistry != null) meterRegistry.counter("auth.refresh.invalid").increment();
            return ResponseEntity.status(401).build();
        }
        boolean blacklisted = tokenBlacklist.isBlacklisted(jwtUtil.getJti(rt));
        if (meterRegistry != null) meterRegistry.counter(blacklisted ? "auth.blacklist.hit" : "auth.blacklist.miss").increment();
        if (blacklisted) {
            if (meterRegistry != null) meterRegistry.counter("auth.refresh.blacklisted").increment();
            return ResponseEntity.status(401).build();
        }
        String username = jwtUtil.getUsername(rt);
        String newAccess = jwtUtil.generateAccessTokenFromUsername(username);
        // rotate refresh token
        String newRefresh = jwtUtil.generateRefreshToken(username);
        tokenBlacklist.blacklist(jwtUtil.getJti(rt), jwtUtil.getRefreshExpirationSeconds());
        if (meterRegistry != null) {
            meterRegistry.counter("auth.refresh.success").increment();
            meterRegistry.counter("auth.refresh.rotate").increment();
            meterRegistry.counter("auth.access.issued", "flow", "refresh").increment();
            meterRegistry.counter("auth.refresh.issued", "flow", "refresh").increment();
            meterRegistry.counter("auth.blacklist.add", "flow", "refresh").increment();
        }
        auditService.record(username, "REFRESH", "rotated", servletRequest.getRemoteAddr(), servletRequest.getHeader("X-Request-Id"));
        return ResponseEntity.ok(new TokenPairResponse(newAccess, newRefresh, jwtUtil.getAccessExpirationSeconds(), jwtUtil.getRefreshExpirationSeconds()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate refresh token (logout)", description = "Blacklists provided refresh token's jti. Idempotent.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Token invalidated or already invalid / not provided"),
        @ApiResponse(responseCode = "401", description = "Malformed token", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> logout(@RequestBody RefreshRequest request, HttpServletRequest servletRequest) {
        String rt = request.getRefreshToken();
        if (rt == null || !jwtUtil.validateJwtToken(rt) || !jwtUtil.isRefreshToken(rt)) {
            if (meterRegistry != null) meterRegistry.counter("auth.logout.invalid").increment();
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        tokenBlacklist.blacklist(jwtUtil.getJti(rt), jwtUtil.getRefreshExpirationSeconds());
        if (meterRegistry != null) {
            meterRegistry.counter("auth.logout.success").increment();
            meterRegistry.counter("auth.blacklist.add", "flow", "logout").increment();
        }
        String username = jwtUtil.getUsername(rt);
        auditService.record(username, "LOGOUT", null, servletRequest.getRemoteAddr(), servletRequest.getHeader("X-Request-Id"));
        return ResponseEntity.noContent().build();
    }
}