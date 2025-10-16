package com.clims.backend.controllers;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.security.JwtUtil;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.services.UserService;
import com.clims.backend.services.RefreshTokenService;
import com.clims.backend.models.entities.RefreshToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    private final RefreshTokenService refreshTokenService;

    // Make LockoutProperties optional so @WebMvcTest slices that don't include configuration properties won't fail
    private com.clims.backend.config.LockoutProperties lockoutProperties;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, CurrentUserService currentUserService, UserService userService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.currentUserService = currentUserService;
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setLockoutProperties(com.clims.backend.config.LockoutProperties lockoutProperties) {
        this.lockoutProperties = lockoutProperties;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
    final int MAX_ATTEMPTS = lockoutProperties != null ? lockoutProperties.getMaxAttempts() : 4;
    final long LOCKOUT_MINUTES = lockoutProperties != null ? lockoutProperties.getMinutes() : 3;

        // Check if user is currently locked
        AppUser maybeUser = userService.findByUsernameOrNull(request.username());
        if (maybeUser != null && userService.isLocked(maybeUser)) {
            return ResponseEntity.status(423).body(Map.of("error", "Account temporarily locked. Try again later."));
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            UserDetails user = (UserDetails) authentication.getPrincipal();
            Map<String, Object> claims = new HashMap<>();
            claims.put("roles", user.getAuthorities());
            String token = jwtUtil.generateToken(user.getUsername(), claims);
            // create refresh token
            AppUser appUser = userService.findByUsername(user.getUsername());
            RefreshToken rt = refreshTokenService.createRefreshToken(appUser);
            // reset failed login counters on successful login
            userService.resetFailedLogins(appUser);
            return ResponseEntity.ok(Map.of("token", token, "refreshToken", rt.getToken()));
        } catch (BadCredentialsException ex) {
            // increment failed attempts and possibly lock
            userService.recordFailedLogin(request.username(), MAX_ATTEMPTS, LOCKOUT_MINUTES);
            AppUser after = userService.findByUsernameOrNull(request.username());
            if (after != null && userService.isLocked(after)) {
                return ResponseEntity.status(423).body(Map.of("error", "Account temporarily locked due to multiple failed login attempts. Try again later."));
            }
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    public record RefreshRequest(String refreshToken) {}

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        var maybe = refreshTokenService.findByToken(req.refreshToken());
        if (maybe.isEmpty()) return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
    RefreshToken rt = maybe.get();
    Map<String, Object> claims = new HashMap<>();
    var role = rt.getUser().getRole();
    if (role != null) claims.put("roles", role);
    else claims.put("roles", List.of());
    String token = jwtUtil.generateToken(rt.getUser().getUsername(), claims);
        return ResponseEntity.ok(Map.of("token", token));
    }

    public record LogoutRequest(String refreshToken) {}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest req) {
        refreshTokenService.findByToken(req.refreshToken()).ifPresent(t -> refreshTokenService.revoke(t));
        return ResponseEntity.ok(Map.of("message", "Logged out"));
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

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody com.clims.backend.dto.UserDtos.ChangePasswordRequest req) {
        AppUser user = currentUserService.requireCurrentUser();
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        try {
            userService.changePassword(user.getId(), req.currentPassword(), req.newPassword());
            // Revoke existing refresh tokens and issue a new one so frontend can replace stored token
            refreshTokenService.revokeForUser(user);
            RefreshToken newRt = refreshTokenService.createRefreshToken(user);
            return ResponseEntity.ok(Map.of("message", "Password changed", "refreshToken", newRt.getToken()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
