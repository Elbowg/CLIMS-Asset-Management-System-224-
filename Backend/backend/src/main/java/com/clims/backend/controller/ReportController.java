package com.clims.backend.controller;

import com.clims.backend.dto.ReportDTO;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.Report;
import com.clims.backend.model.User;
import com.clims.backend.service.ReportService;
import com.clims.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService service;
    private final UserService userService;

    public ReportController(ReportService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping
    public List<ReportDTO> all() { return service.findAll().stream().map(DtoMapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDTO> get(@PathVariable Long id) {
        return service.findById(id).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ReportDTO> create(@Valid @RequestBody ReportDTO dto) {
        Report r = new Report();
        r.setContent(dto.getContent());
        r.setCreatedAt(dto.getCreatedAt());
        if (dto.getGeneratedById() != null) {
            User u = userService.getByIdOrThrow(dto.getGeneratedById());
            r.setGeneratedBy(u);
        }
        Report created = service.create(r);
        return ResponseEntity.created(URI.create("/api/reports/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReportDTO> update(@PathVariable Long id, @Valid @RequestBody ReportDTO dto) {
        return service.findById(id).map(existing -> {
            existing.setContent(dto.getContent());
            existing.setCreatedAt(dto.getCreatedAt());
            if (dto.getGeneratedById() != null) existing.setGeneratedBy(userService.getByIdOrThrow(dto.getGeneratedById())); else existing.setGeneratedBy(null);
            Report updated = service.create(existing);
            return ResponseEntity.ok(DtoMapper.toDto(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
