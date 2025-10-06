package com.clims.backend.controller;

import com.clims.backend.model.Vendor;
import com.clims.backend.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService service;

    public VendorController(VendorService service) { this.service = service; }

    @GetMapping
    public List<Vendor> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> get(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Vendor> create(@Valid @RequestBody Vendor vendor) {
        Vendor created = service.create(vendor);
        return ResponseEntity.created(URI.create("/api/vendors/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendor> update(@PathVariable Long id, @Valid @RequestBody Vendor vendor) {
        return service.update(id, vendor).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}