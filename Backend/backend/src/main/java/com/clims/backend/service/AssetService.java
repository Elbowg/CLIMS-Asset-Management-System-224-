package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Asset;
import com.clims.backend.lifecycle.AssetLifecycle;
import com.clims.backend.model.AssignmentHistory;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AssignmentHistoryRepository;

import java.time.LocalDateTime;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.clims.backend.dto.AssetListProjection;
import com.clims.backend.model.AssetStatus;

@Service
public class AssetService {

    private final AssetRepository repo;
    private final AssignmentHistoryRepository historyRepo;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AssetService(AssetRepository repo, AssignmentHistoryRepository historyRepo, OutboxEventService outboxEventService) {
        this.repo = repo;
        this.historyRepo = historyRepo;
        this.outboxEventService = outboxEventService;
    }

    public List<Asset> findAll() { return repo.findAll(); }

    public Page<AssetListProjection> findPage(Pageable pageable) { return repo.findAllByOrderByIdAsc(pageable); }

    public Page<AssetListProjection> findFilteredPage(AssetStatus status, Long assignedUserId, Pageable pageable) {
        return repo.findFiltered(status, assignedUserId, pageable);
    }

    public Optional<Asset> findById(Long id) { return repo.findById(id); }

    @Cacheable(cacheNames = "assetById", key = "#id")
    public Asset getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset", id));
    }

    @Transactional
    @CachePut(cacheNames = "assetById", key = "#result.id")
    public Asset create(Asset asset) {
        asset.setId(null);
        Asset saved = repo.save(asset);
        try {
            String payload = objectMapper.writeValueAsString(new AssetCreatedPayload(saved.getId(), saved.getName()));
            outboxEventService.record("Asset", String.valueOf(saved.getId()), "AssetCreated", payload,
                    MDC.get("correlationId"), MDC.get("requestId"));
        } catch (Exception e) {
            // Do not fail the main transaction for payload serialization issues: let it bubble if needed
        }
        return saved;
    }

    @CacheEvict(cacheNames = "assetById", key = "#id")
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

    @CachePut(cacheNames = "assetById", key = "#result.id")
    public Asset assignToUser(Asset asset, User user) {
        // Validate transition BEFORE mutating
        AssetStatus from = asset.getStatus();
        AssetLifecycle.validateTransition(from, AssetStatus.ASSIGNED);
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

    @CachePut(cacheNames = "assetById", key = "#result.id")
    public Asset unassignFromUser(Asset asset) {
    AssetStatus from = asset.getStatus();
    AssetLifecycle.validateTransition(from, AssetStatus.AVAILABLE);
    asset.setAssignedUser(null);
    asset.setStatus(AssetStatus.AVAILABLE);
        historyRepo.findTopByAsset_IdOrderByAssignedAtDesc(asset.getId()).ifPresent(last -> {
            last.setUnassignedAt(LocalDateTime.now());
            historyRepo.save(last);
        });
        return repo.save(asset);
    }

    public List<AssignmentHistory> getHistoryForAsset(Long assetId) {
        getByIdOrThrow(assetId); // ensure asset exists
        return historyRepo.findByAsset_IdOrderByAssignedAtDesc(assetId);
    }

    @CacheEvict(cacheNames = "assetById", key = "#id")
    public void deleteById(Long id) { repo.deleteById(id); }

    @CachePut(cacheNames = "assetById", key = "#result.id")
    public Asset moveToMaintenance(Asset asset) {
        AssetLifecycle.validateTransition(asset.getStatus(), AssetStatus.MAINTENANCE);
        asset.setStatus(AssetStatus.MAINTENANCE);
        return repo.save(asset);
    }

    @CachePut(cacheNames = "assetById", key = "#result.id")
    public Asset retire(Asset asset) {
        AssetLifecycle.validateTransition(asset.getStatus(), AssetStatus.RETIRED);
        asset.setStatus(AssetStatus.RETIRED);
        asset.setAssignedUser(null);
        return repo.save(asset);
    }
}

// minimal internal DTO for event payload (avoid leaking entity)
class AssetCreatedPayload {
    public Long id;
    public String name;
    AssetCreatedPayload(Long id, String name) { this.id = id; this.name = name; }
}