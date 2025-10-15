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

    // KPI response for dashboard: total assets, counts by status, upcoming maintenance count
    public record KpiResponse(
            long totalAssets,
            java.util.Map<String, Long> assetsByStatus,
            long upcomingMaintenance
    ) {}
}
