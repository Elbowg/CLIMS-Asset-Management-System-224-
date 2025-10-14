package com.clims.backend.controllers;

import com.clims.backend.dto.MaintenanceDtos;
import com.clims.backend.dto.PageResponse;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.MaintenanceStatus;
import com.clims.backend.services.MaintenanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {
    private final MaintenanceService maintenanceService;
    private final com.clims.backend.security.CurrentUserService currentUserService;

    public MaintenanceController(MaintenanceService maintenanceService, com.clims.backend.security.CurrentUserService currentUserService) {
        this.maintenanceService = maintenanceService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','TECHNICIAN','MANAGER','AUDITOR')")
    public PageResponse<MaintenanceDtos.MaintenanceResponse> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id,desc") String sort,
        @RequestParam(required = false) MaintenanceStatus status,
        @RequestParam(required = false) Long assetId,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo
    ) {
    String[] sortParts = sort.split(",");
    Sort s = sortParts.length == 2 && sortParts[1].equalsIgnoreCase("asc")
        ? Sort.by(sortParts[0]).ascending() : Sort.by(sortParts[0]).descending();
    Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), s);
    Page<Maintenance> result = maintenanceService.search(pageable, status, assetId, dateFrom, dateTo);
    List<MaintenanceDtos.MaintenanceResponse> content = result.getContent().stream().map(this::toResponse).collect(Collectors.toList());
    return new PageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','TECHNICIAN')")
    public ResponseEntity<MaintenanceDtos.MaintenanceResponse> schedule(@Validated @RequestBody MaintenanceDtos.CreateRequest req) {
        Maintenance saved = maintenanceService.schedule(req, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','TECHNICIAN')")
    public ResponseEntity<MaintenanceDtos.MaintenanceResponse> updateStatus(@PathVariable Long id, @Validated @RequestBody MaintenanceDtos.UpdateStatusRequest req) {
        Maintenance saved = maintenanceService.updateStatus(id, req, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','TECHNICIAN','MANAGER','AUDITOR')")
    public ResponseEntity<MaintenanceDtos.MaintenanceResponse> getById(@PathVariable Long id) {
        Maintenance found = maintenanceService.get(id);
        return ResponseEntity.ok(toResponse(found));
    }

    private MaintenanceDtos.MaintenanceResponse toResponse(Maintenance m) {
        return new MaintenanceDtos.MaintenanceResponse(
            m.getId(),
            m.getAsset() != null ? m.getAsset().getId() : null,
            m.getAsset() != null ? m.getAsset().getAssetTag() : null,
            m.getDescription(),
            m.getStatus(),
            m.getScheduledDate(),
            m.getCompletedDate(),
            m.getReportedBy() != null ? m.getReportedBy().getUsername() : null
        );
    }
}
