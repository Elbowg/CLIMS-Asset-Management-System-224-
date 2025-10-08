package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.User;
import com.clims.backend.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.clims.backend.exception.PasswordPolicyException;

@Service
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() { return repo.findAll(); }

    public Optional<User> findById(Long id) { return repo.findById(id); }

    public User getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User create(User user) {
        user.setId(null);
        if (user.getPassword() != null) {
            var raw = user.getPassword();
            if (!isPasswordHash(raw)) { // treat as raw password
                validatePassword(raw);
                user.setPassword(passwordEncoder.encode(raw));
            } else {
                // Defensive: do not allow passing a pre-hashed password directly
                throw new PasswordPolicyException("Raw password required; pre-hashed values are not accepted");
            }
        } else {
            throw new PasswordPolicyException("Password is required");
        }
        return repo.save(user);
    }

    public Optional<User> update(Long id, User updated) {
        return repo.findById(id).map(existing -> {
            existing.setUsername(updated.getUsername());
            existing.setEmail(updated.getEmail());
            existing.setFullName(updated.getFullName());
            existing.setDepartment(updated.getDepartment());
            if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
                var raw = updated.getPassword();
                if (!isPasswordHash(raw)) {
                    validatePassword(raw);
                    existing.setPassword(passwordEncoder.encode(raw));
                } else {
                    throw new PasswordPolicyException("Raw password required; pre-hashed values are not accepted");
                }
            }
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }

    // --- Password policy helpers ---
    private static final Pattern LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern DIGIT = Pattern.compile(".*[0-9].*");

    private void validatePassword(String raw) {
        if (raw.length() < 8) {
            throw new PasswordPolicyException("Password must be at least 8 characters long");
        }
        if (!LETTER.matcher(raw).matches() || !DIGIT.matcher(raw).matches()) {
            throw new PasswordPolicyException("Password must contain at least one letter and one digit");
        }
    }

    private boolean isPasswordHash(String value) {
        // Basic heuristic: BCrypt hashes start with $2a$, $2b$, or $2y$
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}