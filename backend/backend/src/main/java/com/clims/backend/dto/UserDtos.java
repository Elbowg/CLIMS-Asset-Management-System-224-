package com.clims.backend.dto;

import com.clims.backend.security.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserDtos {
    public record RegisterRequest(
            @NotBlank String username,
            @Email String email,
            @NotBlank String password,
            Role role,
            Long departmentId
    ){}

    public record UserResponse(Long id, String username, String email, String role, String department){}

    public record UserListFilter(
            Integer page,
            Integer size,
            String sort,
            Role role,
            Long departmentId,
            String q
    ){}

    public record UpdateRoleRequest(@NotBlank Role role){}
    public record UpdateDepartmentRequest(Long departmentId){}

    public record ResetPasswordRequest(@NotBlank String newPassword){}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword){}
}
