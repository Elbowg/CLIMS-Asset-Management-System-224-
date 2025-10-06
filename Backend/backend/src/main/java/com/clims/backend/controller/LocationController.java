package com.clims.backend.controller;

import com.clims.backend.dto.LocationDTO;
import com.clims.backend.mapper.DtoMapper;
import com.clims.backend.model.Location;
import com.clims.backend.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) { this.service = service; }

    @GetMapping
    public List<LocationDTO> all() { return service.findAll().stream().map(DtoMapper::toDto).collect(Collectors.toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<LocationDTO> get(@PathVariable Long id) {
        return service.findById(id).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LocationDTO> create(@Valid @RequestBody LocationDTO dto) {
        Location l = new Location();
        l.setRoomNumber(dto.getRoomNumber());
        l.setBuilding(dto.getBuilding());
        Location created = service.create(l);
        return ResponseEntity.created(URI.create("/api/locations/" + created.getId())).body(DtoMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LocationDTO> update(@PathVariable Long id, @Valid @RequestBody LocationDTO dto) {
        return service.update(id, mapToEntity(dto)).map(DtoMapper::toDto).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Location mapToEntity(LocationDTO dto) {
        Location l = new Location();
        l.setRoomNumber(dto.getRoomNumber());
        l.setBuilding(dto.getBuilding());
        return l;
    }
}