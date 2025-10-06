package com.clims.backend.controller;

import com.clims.backend.dto.VendorDTO;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.Vendor;
import com.clims.backend.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService service;

    public VendorController(VendorService service) { this.service = service; }

    @GetMapping
    public List<VendorDTO> all() { return service.findAll().stream().map(DtoMapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<VendorDTO> get(@PathVariable Long id) {
        return service.findById(id).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<VendorDTO> create(@Valid @RequestBody VendorDTO dto) {
        Vendor v = new Vendor();
        v.setVendorName(dto.getVendorName());
        v.setContactInfo(dto.getContactInfo());
        Vendor created = service.create(v);
        return ResponseEntity.created(URI.create("/api/vendors/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorDTO> update(@PathVariable Long id, @Valid @RequestBody VendorDTO dto) {
        return service.update(id, mapToEntity(dto)).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Vendor mapToEntity(VendorDTO dto) {
        Vendor v = new Vendor();
        v.setVendorName(dto.getVendorName());
        v.setContactInfo(dto.getContactInfo());
        return v;
    }
}