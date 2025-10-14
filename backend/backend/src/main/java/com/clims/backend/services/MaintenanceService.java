package com.clims.backend.services;

import com.clims.backend.dto.MaintenanceDtos;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.enums.MaintenanceStatus;
import com.clims.backend.repositories.AssetRepository;
import com.clims.backend.repositories.MaintenanceRepository;
import com.clims.backend.exceptions.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MaintenanceService {
    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final AuditLogService auditLogService;

    public MaintenanceService(MaintenanceRepository maintenanceRepository, AssetRepository assetRepository, AuditLogService auditLogService) {
        this.maintenanceRepository = maintenanceRepository;
        this.assetRepository = assetRepository;
        this.auditLogService = auditLogService;
    }

    public List<Maintenance> list() { return maintenanceRepository.findAll(); }
    
    public Page<Maintenance> search(Pageable pageable, MaintenanceStatus status, Long assetId, LocalDate dateFrom, LocalDate dateTo) {
        Specification<Maintenance> spec = Specification.where(null);
        if (status != null) spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        if (assetId != null) spec = spec.and((root, cq, cb) -> cb.equal(root.join("asset").get("id"), assetId));
        if (dateFrom != null) spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("scheduledDate"), dateFrom));
        if (dateTo != null) spec = spec.and((root, cq, cb) -> cb.lessThanOrEqualTo(root.get("scheduledDate"), dateTo));
        return maintenanceRepository.findAll(spec, pageable);
    }

    public Maintenance get(Long id) {
        return maintenanceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance not found"));
    }

    @Transactional
    public Maintenance schedule(MaintenanceDtos.CreateRequest req, AppUser actor) {
    Asset asset = assetRepository.findById(req.assetId()).orElseThrow(() -> new NotFoundException("Asset not found"));
        Maintenance m = new Maintenance();
        m.setAsset(asset);
        m.setDescription(req.description());
        m.setScheduledDate(req.scheduledDate());
        asset.setStatus(AssetStatus.UNDER_REPAIR);
        Maintenance saved = maintenanceRepository.save(m);
        auditLogService.log("Maintenance", saved.getId(), "CREATE", "Maintenance scheduled", actor);
        return saved;
    }

    @Transactional
    public Maintenance updateStatus(Long id, MaintenanceDtos.UpdateStatusRequest req, AppUser actor) {
    Maintenance m = maintenanceRepository.findById(id).orElseThrow(() -> new NotFoundException("Maintenance not found"));
        m.setStatus(req.status());
        m.setCompletedDate(req.completedDate());
        if (req.completedDate() != null) {
            m.getAsset().setStatus(AssetStatus.AVAILABLE);
        }
        auditLogService.log("Maintenance", m.getId(), "UPDATE", "Status changed to " + req.status(), actor);
        return maintenanceRepository.save(m);
    }
}
