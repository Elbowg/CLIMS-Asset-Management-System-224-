package com.clims.backend.controller;

import com.clims.backend.model.Location;
import com.clims.backend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) { this.service = service; }

    @GetMapping
    public List<Location> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Location> get(@PathVariable Long id) {
        return service.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Location> create(@Valid @RequestBody Location loc) {
        Location created = service.create(loc);
        return ResponseEntity.created(URI.create("/api/locations/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Location> update(@PathVariable Long id, @Valid @RequestBody Location loc) {
        return service.update(id, loc).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}