package com.clims.backend.controllers;

import com.clims.backend.dto.AssetDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.services.AssetService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final ModelMapper mapper;

    public AssetController(AssetService assetService, ModelMapper mapper) {
        this.assetService = assetService;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER','AUDITOR')")
    public List<AssetDtos.AssetResponse> list() {
        return assetService.list().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER')")
    public ResponseEntity<AssetDtos.AssetResponse> create(@RequestBody AssetDtos.CreateAssetRequest req) {
        // For simplicity, actor is null here; in production, resolve AppUser by username
        Asset saved = assetService.create(req, null);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER')")
    public ResponseEntity<AssetDtos.AssetResponse> update(@PathVariable Long id, @RequestBody AssetDtos.UpdateAssetRequest req) {
        Asset saved = assetService.update(id, req, null);
        return ResponseEntity.ok(toResponse(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','IT_STAFF','MANAGER')")
    public ResponseEntity<AssetDtos.AssetResponse> assign(@PathVariable Long id, @RequestBody AssetDtos.AssignAssetRequest req) {
        Asset saved = assetService.assign(id, req, null);
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping("/{assetTag}/qr")
    public ResponseEntity<String> getQr(@PathVariable String assetTag) {
        return ResponseEntity.ok(assetService.generateQrForAsset(assetTag));
    }

    private AssetDtos.AssetResponse toResponse(Asset a) {
        return new AssetDtos.AssetResponse(
                a.getId(), a.getAssetTag(), a.getSerialNumber(), a.getMake(), a.getModel(), a.getStatus(),
                a.getAssignedUser() != null ? a.getAssignedUser().getUsername() : null,
                a.getLocation() != null ? a.getLocation().getName() : null,
                a.getVendor() != null ? a.getVendor().getName() : null
        );
    }
}
