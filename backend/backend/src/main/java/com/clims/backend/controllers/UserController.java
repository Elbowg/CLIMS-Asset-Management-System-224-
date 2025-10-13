package com.clims.backend.controllers;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> register(@Validated @RequestBody UserDtos.RegisterRequest req) {
        AppUser created = userService.register(req);
        return ResponseEntity.ok(new UserDtos.UserResponse(created.getId(), created.getUsername(), created.getEmail(), created.getRole().name(), created.getDepartment() != null ? created.getDepartment().getName() : null));
    }
}
