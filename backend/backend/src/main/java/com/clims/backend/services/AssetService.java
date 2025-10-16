package com.clims.backend.services;

import com.clims.backend.dto.AssetDtos;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.models.entities.*;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.repositories.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import com.clims.backend.security.Role;

@Service
public class AssetService {
    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;
    private final DepartmentRepository departmentRepository;
    private final AppUserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ModelMapper mapper;

    public AssetService(AssetRepository assetRepository, LocationRepository locationRepository, VendorRepository vendorRepository, DepartmentRepository departmentRepository, AppUserRepository userRepository, AuditLogService auditLogService, ModelMapper mapper) {
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.mapper = mapper;
    }

    @Transactional
    public Asset create(AssetDtos.CreateAssetRequest req, AppUser actor) {
        Asset asset = new Asset();
        asset.setSerialNumber(req.serialNumber());
        asset.setMake(req.make());
        asset.setModel(req.model());
        asset.setPurchaseDate(req.purchaseDate());
        asset.setWarrantyExpiryDate(req.warrantyExpiryDate());
        if (req.type() != null) asset.setType(req.type());
        if (req.locationId() != null) {
            asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow(() -> new NotFoundException("Location not found")));
        }
        if (req.vendorId() != null) {
            asset.setVendor(vendorRepository.findById(req.vendorId()).orElseThrow(() -> new NotFoundException("Vendor not found")));
        }
        if (req.departmentId() != null) {
            asset.setDepartment(departmentRepository.findById(req.departmentId()).orElseThrow(() -> new NotFoundException("Department not found")));
        }
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssetTag(generateAssetTag());
        Asset saved = assetRepository.save(asset);
        auditLogService.log("Asset", saved.getId(), "CREATE", "Asset created", actor);
        return saved;
    }

    public List<Asset> list() { return assetRepository.findAll(); }

    public Page<Asset> search(Pageable pageable, AssetStatus status, Long departmentId, Long locationId, Long vendorId, String q) {
        Specification<Asset> spec = Specification.where(null);
        if (status != null) spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
    if (departmentId != null) spec = spec.and((root, cq, cb) -> cb.equal(root.join("department", JoinType.LEFT).get("id"), departmentId));
    if (locationId != null) spec = spec.and((root, cq, cb) -> cb.equal(root.join("location", JoinType.LEFT).get("id"), locationId));
    if (vendorId != null) spec = spec.and((root, cq, cb) -> cb.equal(root.join("vendor", JoinType.LEFT).get("id"), vendorId));
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("assetTag")), like),
                    cb.like(cb.lower(root.get("serialNumber")), like),
                    cb.like(cb.lower(root.get("make")), like),
                    cb.like(cb.lower(root.get("model")), like)
            ));
        }
        return assetRepository.findAll(spec, pageable);
    }

    public Asset get(Long id) { return assetRepository.findById(id).orElseThrow(() -> new NotFoundException("Asset not found")); }

    @Transactional
    public Asset update(Long id, AssetDtos.UpdateAssetRequest req, AppUser actor) {
        Asset asset = get(id);
        // Enforce department scope: managers may only update assets in their own department
        if (actor != null && actor.getRole() == Role.MANAGER) {
            Long mgrDept = actor.getDepartment() != null ? actor.getDepartment().getId() : null;
            Long assetDept = asset.getDepartment() != null ? asset.getDepartment().getId() : null;
            if (mgrDept == null || assetDept == null || !mgrDept.equals(assetDept)) {
                throw new AccessDeniedException("Manager cannot update asset in other department");
            }
        }
        if (req.make() != null) asset.setMake(req.make());
        if (req.model() != null) asset.setModel(req.model());
        if (req.warrantyExpiryDate() != null) asset.setWarrantyExpiryDate(req.warrantyExpiryDate());
        if (req.status() != null) asset.setStatus(req.status());
    if (req.type() != null) asset.setType(req.type());
    if (req.locationId() != null) asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow(() -> new NotFoundException("Location not found")));
    if (req.departmentId() != null) asset.setDepartment(departmentRepository.findById(req.departmentId()).orElseThrow(() -> new NotFoundException("Department not found")));
        auditLogService.log("Asset", asset.getId(), "UPDATE", "Asset updated", actor);
        return assetRepository.save(asset);
    }

    @Transactional
    public void delete(Long id, AppUser actor) {
        Asset asset = get(id);
        // Delete authorized at controller via AssetSecurity (service assumes caller checked)
        assetRepository.delete(asset);
        auditLogService.log("Asset", id, "DELETE", "Asset deleted", actor);
    }

    @Transactional
    public Asset assign(Long assetId, AssetDtos.AssignAssetRequest req, AppUser actor) {
        Asset asset = get(assetId);
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE assets can be assigned");
        }
    AppUser assignee = userRepository.findById(req.userId()).orElseThrow(() -> new NotFoundException("User not found"));
        asset.setAssignedUser(assignee);
        asset.setStatus(AssetStatus.ASSIGNED);
        if (req.locationId() != null) {
            asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow(() -> new NotFoundException("Location not found")));
        }
        auditLogService.log("Asset", asset.getId(), "ASSIGN", "Assigned to user " + assignee.getUsername(), actor);
        return assetRepository.save(asset);
    }

    public List<Asset> upcomingWarrantyExpirations(int days) {
        LocalDate by = LocalDate.now().plusDays(days);
        return assetRepository.findByWarrantyExpiryDateBefore(by);
    }

    public String generateAssetTag() {
        // Stable unique ID for tagging, could embed UUID and device info
        return "AST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public byte[] generateQrPng(String assetTag) {
        try {
            var writer = new QRCodeWriter();
            var bitMatrix = writer.encode(assetTag, BarcodeFormat.QR_CODE, 200, 200);
            var baos = new java.io.ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return baos.toByteArray();
        } catch (WriterException | java.io.IOException e) {
            throw new RuntimeException("Failed to generate QR", e);
        }
    }

    @Transactional
    public Asset dispose(Long id, AppUser actor) {
        Asset asset = get(id);
        asset.setStatus(AssetStatus.RETIRED);
        asset.setAssignedUser(null);
        auditLogService.log("Asset", id, "DISPOSE", "Asset retired", actor);
        return assetRepository.save(asset);
    }

    // Note: authorization is enforced by controller-level AssetSecurity bean; service retains business validation and auditing.
}
