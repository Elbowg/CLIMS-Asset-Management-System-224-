package com.clims.backend.controller;

import com.clims.backend.dto.MaintenanceDTO;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.Maintenance;
import com.clims.backend.model.User;
import com.clims.backend.model.Asset;
import com.clims.backend.service.AssetService;
import com.clims.backend.service.MaintenanceService;
import com.clims.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService service;
    private final AssetService assetService;
    private final UserService userService;

    public MaintenanceController(MaintenanceService service, AssetService assetService, UserService userService) {
        this.service = service;
        this.assetService = assetService;
        this.userService = userService;
    }

    @GetMapping
    public List<MaintenanceDTO> all() { return service.findAll().stream().map(DtoMapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> get(@PathVariable Long id) {
        return service.findById(id).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MaintenanceDTO> create(@Valid @RequestBody MaintenanceDTO dto) {
        Maintenance m = new Maintenance();
        m.setDescription(dto.getDescription());
        m.setReportedAt(dto.getReportedAt());
        m.setResolution(dto.getResolution());
        m.setResolvedAt(dto.getResolvedAt());

        if (dto.getAssetId() != null) {
            Asset a = assetService.getByIdOrThrow(dto.getAssetId());
            m.setAsset(a);
        }

        if (dto.getReportedById() != null) {
            User u = userService.getByIdOrThrow(dto.getReportedById());
            m.setReportedBy(u);
        }

        Maintenance created = service.create(m);
        return ResponseEntity.created(URI.create("/api/maintenance/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceDTO> update(@PathVariable Long id, @Valid @RequestBody MaintenanceDTO dto) {
        return service.findById(id).map(existing -> {
            existing.setDescription(dto.getDescription());
            existing.setReportedAt(dto.getReportedAt());
            existing.setResolution(dto.getResolution());
            existing.setResolvedAt(dto.getResolvedAt());

            if (dto.getAssetId() != null) existing.setAsset(assetService.getByIdOrThrow(dto.getAssetId())); else existing.setAsset(null);
            if (dto.getReportedById() != null) existing.setReportedBy(userService.getByIdOrThrow(dto.getReportedById())); else existing.setReportedBy(null);

            Maintenance updated = service.create(existing);
            return ResponseEntity.ok(DtoMapper.toDto(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
