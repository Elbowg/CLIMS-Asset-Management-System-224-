package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Location;
import com.clims.backend.Repository.LocationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationService {

    private final LocationRepository repo;

    public LocationService(LocationRepository repo) { this.repo = repo; }

    public List<Location> findAll() { return repo.findAll(); }

    public Optional<Location> findById(Long id) { return repo.findById(id); }

    public Location getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }

    public Location create(Location loc) {
        loc.setId(null);
        return repo.save(loc);
    }

    public Optional<Location> update(Long id, Location updated) {
        return repo.findById(id).map(existing -> {
            existing.setRoomNumber(updated.getRoomNumber());
            existing.setBuilding(updated.getBuilding());
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }
}