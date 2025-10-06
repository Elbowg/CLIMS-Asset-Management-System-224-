package com.clims.backend.mapper;

import com.clims.backend.dto.*;
import com.clims.backend.model.*;

public final class DtoMapper {

    private DtoMapper() {}

    public static AssetDTO toDto(Asset a) {
        if (a == null) return null;
        AssetDTO d = new AssetDTO();
        d.setId(a.getId());
        d.setName(a.getName());
        d.setType(a.getType());
        d.setSerialNumber(a.getSerialNumber());
        d.setStatus(a.getStatus());
        d.setPurchaseDate(a.getPurchaseDate());
        d.setAssignedUserId(a.getAssignedUser() != null ? a.getAssignedUser().getId() : null);
        d.setLocationId(a.getLocation() != null ? a.getLocation().getId() : null);
        d.setVendorId(a.getVendor() != null ? a.getVendor().getId() : null);
        return d;
    }

    public static UserDTO toDto(User u) {
        if (u == null) return null;
        UserDTO d = new UserDTO();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setEmail(u.getEmail());
        d.setFullName(u.getFullName());
        d.setDepartmentId(u.getDepartmentId());
        return d;
    }

    public static VendorDTO toDto(Vendor v) {
        if (v == null) return null;
        VendorDTO d = new VendorDTO();
        d.setId(v.getId());
        d.setVendorName(v.getVendorName());
        d.setContactInfo(v.getContactInfo());
        return d;
    }

    public static LocationDTO toDto(Location l) {
        if (l == null) return null;
        LocationDTO d = new LocationDTO();
        d.setId(l.getId());
        d.setRoomNumber(l.getRoomNumber());
        d.setBuilding(l.getBuilding());
        return d;
    }

    public static MaintenanceDTO toDto(Maintenance m) {
        if (m == null) return null;
        MaintenanceDTO d = new MaintenanceDTO();
        d.setId(m.getId());
        d.setAssetId(m.getAsset() != null ? m.getAsset().getId() : null);
        d.setReportedById(m.getReportedBy() != null ? m.getReportedBy().getId() : null);
        d.setDescription(m.getDescription());
        d.setReportedAt(m.getReportedAt());
        d.setResolution(m.getResolution());
        d.setResolvedAt(m.getResolvedAt());
        return d;
    }

    public static ReportDTO toDto(Report r) {
        if (r == null) return null;
        ReportDTO d = new ReportDTO();
        d.setId(r.getId());
        d.setGeneratedById(r.getGeneratedBy() != null ? r.getGeneratedBy().getId() : null);
        d.setContent(r.getContent());
        d.setCreatedAt(r.getCreatedAt());
        return d;
    }
}