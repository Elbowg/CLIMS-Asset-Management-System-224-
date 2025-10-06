package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Report;
import com.clims.backend.Repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportService {

    private final ReportRepository repo;

    public ReportService(ReportRepository repo) { this.repo = repo; }

    public List<Report> findAll() { return repo.findAll(); }

    public Optional<Report> findById(Long id) { return repo.findById(id); }

    public Report getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report", id));
    }

    public Report create(Report r) {
        r.setId(null);
        if (r.getCreatedAt() == null) r.setCreatedAt(LocalDateTime.now());
        return repo.save(r);
    }

    public Optional<Report> update(Long id, Report updated) {
        return repo.findById(id).map(existing -> {
            existing.setGeneratedBy(updated.getGeneratedBy());
            existing.setContent(updated.getContent());
            existing.setCreatedAt(updated.getCreatedAt());
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }
}
