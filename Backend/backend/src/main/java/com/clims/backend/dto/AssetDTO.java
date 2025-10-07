package com.clims.backend.dto;

import java.time.LocalDate;
import com.clims.backend.model.AssetType;
import com.clims.backend.model.AssetStatus;

public class AssetDTO {
    private Long id;
    private String name;
    private AssetType type;
    private String serialNumber;
    private AssetStatus status;
    private Long assignedUserId;
    private Long locationId;
    private Long vendorId;
    private LocalDate purchaseDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AssetType getType() { return type; }
    public void setType(AssetType type) { this.type = type; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public Long getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(Long assignedUserId) { this.assignedUserId = assignedUserId; }
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public Long getVendorId() { return vendorId; }
    public void setVendorId(Long vendorId) { this.vendorId = vendorId; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
}