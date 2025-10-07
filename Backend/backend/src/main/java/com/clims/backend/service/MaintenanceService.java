package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Maintenance;
import com.clims.backend.repository.MaintenanceRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceService {

    private final MaintenanceRepository repo;

    public MaintenanceService(MaintenanceRepository repo) { this.repo = repo; }

    public List<Maintenance> findAll() { return repo.findAll(); }

    public Optional<Maintenance> findById(Long id) { return repo.findById(id); }

    public Maintenance getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Maintenance", id));
    }

    public Maintenance create(Maintenance m) {
        m.setId(null);
        if (m.getReportedAt() == null) m.setReportedAt(LocalDateTime.now());
        return repo.save(m);
    }

    public Optional<Maintenance> update(Long id, Maintenance updated) {
        return repo.findById(id).map(existing -> {
            existing.setAsset(updated.getAsset());
            existing.setReportedBy(updated.getReportedBy());
            existing.setDescription(updated.getDescription());
            existing.setReportedAt(updated.getReportedAt());
            existing.setResolution(updated.getResolution());
            existing.setResolvedAt(updated.getResolvedAt());
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }
}
