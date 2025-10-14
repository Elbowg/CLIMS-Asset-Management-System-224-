package com.clims.backend.services;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.DepartmentRepository;
import org.modelmapper.ModelMapper;
import com.clims.backend.exceptions.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clims.backend.security.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UserService {
    private final AppUserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;

    public UserService(AppUserRepository userRepository, DepartmentRepository departmentRepository, PasswordEncoder passwordEncoder, ModelMapper mapper) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.mapper = mapper;
    }

    @Transactional
    public AppUser register(UserDtos.RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new IllegalStateException("Username already in use");
        }
        if (req.email() != null && userRepository.existsByEmail(req.email())) {
            throw new IllegalStateException("Email already in use");
        }
        AppUser user = new AppUser();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(req.role() != null ? req.role() : Role.EMPLOYEE);
        if (req.departmentId() != null) {
        Department dept = departmentRepository.findById(req.departmentId())
            .orElseThrow(() -> new NotFoundException("Department not found"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }

    public AppUser get(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public Page<AppUser> search(Pageable pageable, Role role, Long departmentId, String q) {
        // Simple filtering using repository methods or in-memory filter fallback
        // For now, fetch page and filter stream as needed (could be optimized with specs/queries)
        Page<AppUser> page = userRepository.findAll(pageable);
        if (role == null && departmentId == null && (q == null || q.isBlank())) {
            return page;
        }
        java.util.List<AppUser> filtered = page.getContent().stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> departmentId == null || (u.getDepartment() != null && departmentId.equals(u.getDepartment().getId())))
                .filter(u -> q == null || q.isBlank() || u.getUsername().toLowerCase().contains(q.toLowerCase()) || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q.toLowerCase())))
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
    }

    @Transactional
    public AppUser updateRole(Long id, Role role) {
        AppUser user = get(id);
        if (role == null) throw new IllegalArgumentException("Role is required");
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public AppUser updateDepartment(Long id, Long departmentId) {
        AppUser user = get(id);
        if (departmentId == null) {
            user.setDepartment(null);
        } else {
            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new NotFoundException("Department not found"));
            user.setDepartment(dept);
        }
        return userRepository.save(user);
    }
}
