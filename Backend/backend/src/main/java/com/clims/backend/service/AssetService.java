package com.clims.backend.service;

import com.clims.backend.model.Asset;
import com.clims.backend.Repository.AssetRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssetService {

    private final AssetRepository repo;

    public AssetService(AssetRepository repo) {
        this.repo = repo;
    }

    public List<Asset> findAll() { return repo.findAll(); }

    public Optional<Asset> findById(Long id) { return repo.findById(id); }

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

    public void delete(Long id) { repo.deleteById(id); }
}