package com.clims.backend.controller;

import com.clims.backend.dto.UserDTO;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.User;
import com.clims.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    public List<UserDTO> all() { return service.findAll().stream().map(DtoMapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> get(@PathVariable Long id) {
        return service.findById(id).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserDTO dto) {
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setFullName(dto.getFullName());
        u.setDepartmentId(dto.getDepartmentId());
        User created = service.create(u);
        return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        return service.update(id, mapToEntity(dto)).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private User mapToEntity(UserDTO dto) {
        User u = new User();
        u.setUsername(dto.getUsername());
        u.setEmail(dto.getEmail());
        u.setFullName(dto.getFullName());
        u.setDepartmentId(dto.getDepartmentId());
        return u;
    }
}