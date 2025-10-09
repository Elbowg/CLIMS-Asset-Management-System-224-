package com.clims.backend.controller;

import com.clims.backend.dto.AssetDTO;
import com.clims.backend.dto.AssignmentHistoryDTO;
import com.clims.backend.dto.AssignRequest;
import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.Asset;
import com.clims.backend.model.Location;
import com.clims.backend.model.User;
import com.clims.backend.model.Vendor;
import com.clims.backend.service.AssetService;
import com.clims.backend.service.LocationService;
import com.clims.backend.service.UserService;
import com.clims.backend.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final UserService userService;
    private final LocationService locationService;
    private final VendorService vendorService;

    public AssetController(AssetService assetService,
                           UserService userService,
                           LocationService locationService,
                           VendorService vendorService) {
        this.assetService = assetService;
        this.userService = userService;
        this.locationService = locationService;
        this.vendorService = vendorService;
    }

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<?> all(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                       @RequestParam(name = "size", required = false) Integer size,
                       @RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "assignedUserId", required = false) Long assignedUserId,
                       @RequestParam(name = "sort", required = false) String sort) {
        int effectiveSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, effectiveSize, resolveSort(sort));
        // Parse status enum safely
        com.clims.backend.model.AssetStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try { statusEnum = com.clims.backend.model.AssetStatus.valueOf(status.toUpperCase()); }
            catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Invalid status value: " + status); }
        }
        return assetService.findFilteredPage(statusEnum, assignedUserId, pageable);
    }

    private Sort resolveSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        // Support comma form field,dir e.g. name,desc
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        String dir = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        // Whitelist allowed fields; on invalid field, hard fallback to id ASC
        if (!field.matches("id|name|status|assignedUserId")) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        // Default to ASC when direction is invalid or unspecified
        Sort.Direction direction = dir.equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        // assignedUserId is nested; map to assignedUser.id for JPA
        if (field.equals("assignedUserId")) {
            return Sort.by(direction, "assignedUser.id");
        }
        return Sort.by(direction, field);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<AssetDTO> get(@PathVariable Long id) {
    Asset asset = assetService.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    return ResponseEntity.ok(DtoMapper.toDto(asset));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDTO> create(@Valid @RequestBody AssetDTO dto) {
        Asset a = new Asset();
        a.setName(dto.getName());
        if (dto.getType() != null) {
            a.setType(dto.getType());
        }
        a.setSerialNumber(dto.getSerialNumber());
        if (dto.getStatus() != null) {
            a.setStatus(dto.getStatus());
        }
        a.setPurchaseDate(dto.getPurchaseDate());

        if (dto.getAssignedUserId() != null) {
            User u = userService.findById(dto.getAssignedUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", dto.getAssignedUserId()));
            a.setAssignedUser(u);
        }
        if (dto.getLocationId() != null) {
            Location l = locationService.findById(dto.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location", dto.getLocationId()));
            a.setLocation(l);
        }
        if (dto.getVendorId() != null) {
            Vendor v = vendorService.findById(dto.getVendorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor", dto.getVendorId()));
            a.setVendor(v);
        }

        Asset created = assetService.create(a);
        return ResponseEntity.created(URI.create("/api/assets/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDTO> update(@PathVariable Long id, @Valid @RequestBody AssetDTO dto) {
        Asset existing = assetService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        {
            existing.setName(dto.getName());
            if (dto.getType() != null) {
                existing.setType(dto.getType());
            } else {
                existing.setType(null);
            }
            existing.setSerialNumber(dto.getSerialNumber());
            if (dto.getStatus() != null) {
                existing.setStatus(dto.getStatus());
            } else {
                existing.setStatus(null);
            }
            existing.setPurchaseDate(dto.getPurchaseDate());

            if (dto.getAssignedUserId() != null) {
                User u = userService.findById(dto.getAssignedUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("User", dto.getAssignedUserId()));
                existing.setAssignedUser(u);
            } else {
                existing.setAssignedUser(null);
            }

            if (dto.getLocationId() != null) {
                Location l = locationService.findById(dto.getLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Location", dto.getLocationId()));
                existing.setLocation(l);
            } else {
                existing.setLocation(null);
            }

            if (dto.getVendorId() != null) {
                Vendor v = vendorService.findById(dto.getVendorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Vendor", dto.getVendorId()));
                existing.setVendor(v);
            } else {
                existing.setVendor(null);
            }

            Asset saved = assetService.create(existing); // TODO: Replace with assetService.update if/when implemented
            return ResponseEntity.ok(DtoMapper.toDto(saved));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDTO> assign(@PathVariable Long id, @RequestBody AssignRequest req) {
        Asset asset = assetService.getByIdOrThrow(id);
        User user = userService.findById(req.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User", req.getUserId()));
        Asset updated = assetService.assignToUser(asset, user);
        return ResponseEntity.ok(DtoMapper.toDto(updated));
    }

    @PostMapping("/{id}/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDTO> unassign(@PathVariable Long id) {
        Asset asset = assetService.getByIdOrThrow(id);
        Asset updated = assetService.unassignFromUser(asset);
        return ResponseEntity.ok(DtoMapper.toDto(updated));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<List<AssignmentHistoryDTO>> history(@PathVariable Long id) {
        // Will throw if asset not found
        List<AssignmentHistoryDTO> list = assetService.getHistoryForAsset(id).stream()
                .map(DtoMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}