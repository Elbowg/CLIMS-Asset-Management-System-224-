package com.clims.backend.dto;

import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.enums.MaintenanceStatus;

import java.time.LocalDate;

public class ReportDtos {
    public enum ReportFormat { CSV, PDF }

    public record InventoryFilter(
            Long departmentId,
            AssetStatus status,
            Long vendorId,
            LocalDate purchasedFrom,
            LocalDate purchasedTo
    ) {}

    public record MaintenanceFilter(
            Long assetId,
            MaintenanceStatus status,
            LocalDate from,
            LocalDate to
    ) {}
}
