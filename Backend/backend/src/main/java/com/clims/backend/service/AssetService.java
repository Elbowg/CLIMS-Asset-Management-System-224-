package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Asset;
import com.clims.backend.model.AssignmentHistory;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AssignmentHistoryRepository;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    private final AssetRepository repo;
    private final AssignmentHistoryRepository historyRepo;

    public AssetService(AssetRepository repo, AssignmentHistoryRepository historyRepo) {
        this.repo = repo;
        this.historyRepo = historyRepo;
    }

    public List<Asset> findAll() { return repo.findAll(); }

    public Optional<Asset> findById(Long id) { return repo.findById(id); }

    public Asset getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }

    public Asset create(Asset asset) {
        asset.setId(null);
        return repo.save(asset);
    }

    public Optional<Asset> update(Long id, Asset updated) {
        return repo.findById(id).map(existing -> {
            existing.setName(updated.getName());
            existing.setType(updated.getType());
            existing.setSerialNumber(updated.getSerialNumber());
            existing.setStatus(updated.getStatus());
            existing.setAssignedUser(updated.getAssignedUser());
            existing.setLocation(updated.getLocation());
            existing.setVendor(updated.getVendor());
            existing.setPurchaseDate(updated.getPurchaseDate());
            return repo.save(existing);
        });
    }

    public Asset save(Asset asset) { return repo.save(asset); }

    public Asset assignToUser(Asset asset, User user) {
        if (asset.getStatus() == AssetStatus.MAINTENANCE || asset.getStatus() == AssetStatus.RETIRED) {
            throw new IllegalStateException("Asset not available for assignment: " + asset.getId());
        }
        asset.setAssignedUser(user);
        asset.setStatus(AssetStatus.ASSIGNED);
        Asset saved = repo.save(asset);

        AssignmentHistory h = new AssignmentHistory();
        h.setAsset(saved);
        h.setUser(user);
        h.setAssignedAt(LocalDateTime.now());
        historyRepo.save(h);
        return saved;
    }

    public Asset unassignFromUser(Asset asset) {
        asset.setAssignedUser(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        historyRepo.findTopByAsset_IdOrderByAssignedAtDesc(asset.getId()).ifPresent(last -> {
            last.setUnassignedAt(LocalDateTime.now());
            historyRepo.save(last);
        });
        return repo.save(asset);
    }

    public void deleteById(Long id) { repo.deleteById(id); }
}