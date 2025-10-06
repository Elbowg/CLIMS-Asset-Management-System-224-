package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.User;
import com.clims.backend.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) { this.repo = repo; }

    public List<User> findAll() { return repo.findAll(); }

    public Optional<User> findById(Long id) { return repo.findById(id); }

    public User getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User create(User user) {
        user.setId(null);
        return repo.save(user);
    }

    public Optional<User> update(Long id, User updated) {
        return repo.findById(id).map(existing -> {
            existing.setUsername(updated.getUsername());
            existing.setEmail(updated.getEmail());
            existing.setFullName(updated.getFullName());
            existing.setDepartmentId(updated.getDepartmentId());
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }
}