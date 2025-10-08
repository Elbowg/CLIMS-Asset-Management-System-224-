package com.clims.backend.controller;

import com.clims.backend.dto.AuthRequest;
import com.clims.backend.dto.TokenPairResponse;
import com.clims.backend.dto.RefreshRequest;
import com.clims.backend.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> authenticateUser(@RequestBody AuthRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String access = jwtUtil.generateAccessToken(authentication);
        String refresh = jwtUtil.generateRefreshToken(loginRequest.getUsername());
        return ResponseEntity.ok(new TokenPairResponse(access, refresh, jwtUtil.getAccessExpirationSeconds(), jwtUtil.getRefreshExpirationSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshRequest request) {
        String rt = request.getRefreshToken();
        if (rt == null || !jwtUtil.validateJwtToken(rt) || !jwtUtil.isRefreshToken(rt)) {
            return ResponseEntity.status(401).build();
        }
        String username = jwtUtil.getUsername(rt);
        String newAccess = jwtUtil.generateAccessTokenFromUsername(username);
        // rotate refresh token
        String newRefresh = jwtUtil.generateRefreshToken(username);
        return ResponseEntity.ok(new TokenPairResponse(newAccess, newRefresh, jwtUtil.getAccessExpirationSeconds(), jwtUtil.getRefreshExpirationSeconds()));
    }
}