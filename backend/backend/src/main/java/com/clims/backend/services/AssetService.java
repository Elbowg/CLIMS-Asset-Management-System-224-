package com.clims.backend.services;

import com.clims.backend.dto.AssetDtos;
import com.clims.backend.models.entities.*;
import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.repositories.*;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {
    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;
    private final AppUserRepository userRepository;
    private final AuditLogService auditLogService;
    private final ModelMapper mapper;

    public AssetService(AssetRepository assetRepository, LocationRepository locationRepository, VendorRepository vendorRepository, AppUserRepository userRepository, AuditLogService auditLogService, ModelMapper mapper) {
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
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
        if (req.locationId() != null) {
            asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow());
        }
        if (req.vendorId() != null) {
            asset.setVendor(vendorRepository.findById(req.vendorId()).orElseThrow());
        }
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setAssetTag(generateAssetTag());
        Asset saved = assetRepository.save(asset);
        auditLogService.log("Asset", saved.getId(), "CREATE", "Asset created", actor);
        return saved;
    }

    public List<Asset> list() { return assetRepository.findAll(); }

    public Asset get(Long id) { return assetRepository.findById(id).orElseThrow(); }

    @Transactional
    public Asset update(Long id, AssetDtos.UpdateAssetRequest req, AppUser actor) {
        Asset asset = get(id);
        if (req.make() != null) asset.setMake(req.make());
        if (req.model() != null) asset.setModel(req.model());
        if (req.warrantyExpiryDate() != null) asset.setWarrantyExpiryDate(req.warrantyExpiryDate());
        if (req.status() != null) asset.setStatus(req.status());
        if (req.locationId() != null) asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow());
        auditLogService.log("Asset", asset.getId(), "UPDATE", "Asset updated", actor);
        return assetRepository.save(asset);
    }

    @Transactional
    public void delete(Long id, AppUser actor) {
        Asset asset = get(id);
        assetRepository.delete(asset);
        auditLogService.log("Asset", id, "DELETE", "Asset deleted", actor);
    }

    @Transactional
    public Asset assign(Long assetId, AssetDtos.AssignAssetRequest req, AppUser actor) {
        Asset asset = get(assetId);
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE assets can be assigned");
        }
        AppUser assignee = userRepository.findById(req.userId()).orElseThrow();
        asset.setAssignedUser(assignee);
        asset.setStatus(AssetStatus.ASSIGNED);
        if (req.locationId() != null) {
            asset.setLocation(locationRepository.findById(req.locationId()).orElseThrow());
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

    public String generateQrForAsset(String assetTag) {
        try {
            var writer = new QRCodeWriter();
            var bitMatrix = writer.encode(assetTag, BarcodeFormat.QR_CODE, 200, 200);
            // Convert to simple string bytes (for demo). For real use, render PNG bytes.
            String payload = assetTag + ":" + bitMatrix.getWidth() + "x" + bitMatrix.getHeight();
            return Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        } catch (WriterException e) {
            throw new RuntimeException("Failed to generate QR", e);
        }
    }
}
