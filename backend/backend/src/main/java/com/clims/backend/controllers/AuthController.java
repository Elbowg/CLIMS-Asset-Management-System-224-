package com.clims.backend.controllers;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, CurrentUserService currentUserService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserDetails user = (UserDetails) authentication.getPrincipal();
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getAuthorities());
            String token = jwtUtil.generateToken(user.getUsername(), claims);
            return ResponseEntity.ok(Map.of("token", token));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated @RequestBody UserDtos.RegisterRequest request) {
        AppUser created = userService.register(request);
        return ResponseEntity.status(201).body(new UserDtos.UserResponse(
                created.getId(),
                created.getUsername(),
                created.getEmail(),
                created.getRole() != null ? created.getRole().name() : null,
                created.getDepartment() != null ? created.getDepartment().getName() : null
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        AppUser user = currentUserService.requireCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(new UserDtos.UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null
        ));
    }
}
