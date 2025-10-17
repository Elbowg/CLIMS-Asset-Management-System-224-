package com.clims.backend.controllers;

import com.clims.backend.dto.AssetDtos;
import com.clims.backend.dto.PageResponse;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.dto.MaintenanceDtos;
import com.clims.backend.models.enums.MaintenanceStatus;
import com.clims.backend.security.CurrentUserService;
import com.clims.backend.services.AssetService;
import com.clims.backend.services.MaintenanceService;
import com.clims.backend.services.AuditLogService;
import com.clims.backend.models.entities.AuditLog;
import org.modelmapper.ModelMapper;
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

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final ModelMapper mapper;
    private final CurrentUserService currentUserService;
    private final MaintenanceService maintenanceService;
    private final AuditLogService auditLogService;

    public AssetController(AssetService assetService, ModelMapper mapper, CurrentUserService currentUserService, MaintenanceService maintenanceService, AuditLogService auditLogService) {
        this.assetService = assetService;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.maintenanceService = maintenanceService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER','AUDITOR')")
    public ResponseEntity<AssetDtos.AssetResponse> getById(@PathVariable Long id) {
        Asset asset = assetService.get(id);
        return ResponseEntity.ok(toResponse(asset));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER','AUDITOR')")
    public PageResponse<AssetDtos.AssetResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String q
    ) {
        String[] sortParts = sort.split(",");
        Sort s = sortParts.length == 2 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending() : Sort.by(sortParts[0]).descending();
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), s);
        Page<Asset> result = assetService.search(pageable, status, departmentId, locationId, vendorId, q);
        List<AssetDtos.AssetResponse> content = result.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    @PostMapping
    @PreAuthorize("@assetSecurity.canCreate(@currentUserService.requireCurrentUser(), #req.departmentId)")
    public ResponseEntity<AssetDtos.AssetResponse> create(@Validated @RequestBody AssetDtos.CreateAssetRequest req) {
        Asset saved = assetService.create(req, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@assetSecurity.canModify(@currentUserService.requireCurrentUser(), @assetService.get(#id))")
    public ResponseEntity<AssetDtos.AssetResponse> update(@PathVariable Long id, @Validated @RequestBody AssetDtos.UpdateAssetRequest req) {
        Asset saved = assetService.update(id, req, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@assetSecurity.isAdmin(@currentUserService.requireCurrentUser())")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id, currentUserService.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("@assetSecurity.canModify(@currentUserService.requireCurrentUser(), @assetService.get(#id))")
    public ResponseEntity<AssetDtos.AssetResponse> assign(@PathVariable Long id, @Validated @RequestBody AssetDtos.AssignAssetRequest req) {
        Asset saved = assetService.assign(id, req, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/{id}/dispose")
    @PreAuthorize("@assetSecurity.canDispose(@currentUserService.requireCurrentUser(), @assetService.get(#id))")
    public ResponseEntity<AssetDtos.AssetResponse> dispose(@PathVariable Long id) {
        Asset saved = assetService.dispose(id, currentUserService.requireCurrentUser());
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping("/{assetTag}/qr")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER','AUDITOR')")
    public ResponseEntity<?> getQr(@PathVariable String assetTag, @RequestParam(value = "dataUrl", defaultValue = "false") boolean dataUrl) {
        byte[] png = assetService.generateQrPng(assetTag);
        if (dataUrl) {
            String base64 = java.util.Base64.getEncoder().encodeToString(png);
            return ResponseEntity.ok("data:image/png;base64," + base64);
        }
        return ResponseEntity
                .ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, org.springframework.http.MediaType.IMAGE_PNG_VALUE)
                .body(png);
    }

    private AssetDtos.AssetResponse toResponse(Asset a) {
        return new AssetDtos.AssetResponse(
                a.getId(), a.getAssetTag(), a.getSerialNumber(), a.getMake(), a.getModel(), a.getStatus(),
                a.getType(),
        a.getAssignedUser() != null ? a.getAssignedUser().getUsername() : null,
        a.getLocation() != null ? a.getLocation().getName() : null,
        a.getVendor() != null ? a.getVendor().getName() : null,
        a.getDepartment() != null ? a.getDepartment().getId() : null,
        a.getDepartment() != null ? a.getDepartment().getName() : null
        );
    }

    @GetMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','TECHNICIAN','MANAGER','AUDITOR')")
    public PageResponse<MaintenanceDtos.MaintenanceResponse> maintenanceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String sort,
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) java.time.LocalDate dateFrom,
            @RequestParam(required = false) java.time.LocalDate dateTo
    ) {
        // Ensure asset exists or throw 404
        assetService.get(id);
        String[] sortParts = sort.split(",");
        Sort s = sortParts.length == 2 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.by(sortParts[0]).ascending() : Sort.by(sortParts[0]).descending();
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), s);
        Page<Maintenance> result = maintenanceService.search(pageable, status, id, dateFrom, dateTo);
        List<MaintenanceDtos.MaintenanceResponse> content = result.getContent().stream().map(this::toMaintenanceResponse).collect(Collectors.toList());
        return new PageResponse<>(content, result.getTotalElements(), result.getTotalPages(), result.getNumber(), result.getSize());
    }

    private MaintenanceDtos.MaintenanceResponse toMaintenanceResponse(Maintenance m) {
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

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','MANAGER','IT_STAFF')")
    public ResponseEntity<List<AuditLogResponse>> audit(@PathVariable Long id) {
        // ensure asset exists, will throw 404 if not
        assetService.get(id);
        List<AuditLog> logs = auditLogService.findByEntity("Asset", id);
        List<AuditLogResponse> body = logs.stream().map(this::toAuditResponse).collect(Collectors.toList());
        return ResponseEntity.ok(body);
    }

    private AuditLogResponse toAuditResponse(AuditLog l) {
        return new AuditLogResponse(
                l.getId(), l.getEntityName(), l.getEntityId(), l.getAction(), l.getDetails(),
                l.getCreatedAt(), l.getUser() != null ? l.getUser().getUsername() : null
        );
    }

    public record AuditLogResponse(
            Long id,
            String entityName,
            Long entityId,
            String action,
            String details,
            java.time.Instant createdAt,
            String actor
    ) {}
}
