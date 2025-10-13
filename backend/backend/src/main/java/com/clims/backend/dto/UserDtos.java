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
}
