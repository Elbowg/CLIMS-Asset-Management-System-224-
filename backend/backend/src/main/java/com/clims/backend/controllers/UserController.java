package com.clims.backend.controllers;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.services.UserService;
import org.springframework.http.ResponseEntity;
import java.util.Map;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> getById(@PathVariable Long id) {
        AppUser u = userService.get(id);
        return ResponseEntity.ok(new UserDtos.UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.getDepartment() != null ? u.getDepartment().getName() : null));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDtos.UserResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) com.clims.backend.security.Role role,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String q
    ) {
        String[] sortParts = sort.split(",");
        Sort s = sortParts.length == 2 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending() : Sort.by(sortParts[0]).descending();
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), s);
        Page<AppUser> result = userService.search(pageable, role, departmentId, q);
        java.util.List<UserDtos.UserResponse> content = result.getContent().stream()
                .map(u -> new UserDtos.UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.getDepartment() != null ? u.getDepartment().getName() : null))
                .toList();
        return new PageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> updateRole(@PathVariable Long id, @RequestBody UserDtos.UpdateRoleRequest req) {
        AppUser updated = userService.updateRole(id, req.role());
        return ResponseEntity.ok(new UserDtos.UserResponse(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getDepartment() != null ? updated.getDepartment().getName() : null));
    }

    @PatchMapping("/{id}/department")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> updateDepartment(@PathVariable Long id, @RequestBody UserDtos.UpdateDepartmentRequest req) {
        AppUser updated = userService.updateDepartment(id, req.departmentId());
        return ResponseEntity.ok(new UserDtos.UserResponse(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getDepartment() != null ? updated.getDepartment().getName() : null));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody UserDtos.ResetPasswordRequest req) {
        try {
            userService.resetPassword(id, req.newPassword());
            return ResponseEntity.ok(Map.of("message", "Password reset"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        AppUser updated = userService.unlockUser(id);
        return ResponseEntity.ok(new UserDtos.UserResponse(updated.getId(), updated.getUsername(), updated.getEmail(), updated.getRole().name(), updated.getDepartment() != null ? updated.getDepartment().getName() : null));
    }
}
