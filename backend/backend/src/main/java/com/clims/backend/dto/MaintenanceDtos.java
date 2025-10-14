package com.clims.backend.dto;

import com.clims.backend.models.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class MaintenanceDtos {
    public record CreateRequest(
            @NotNull Long assetId,
            @NotBlank String description,
            LocalDate scheduledDate
    ){}

    public record UpdateStatusRequest(
            @NotNull MaintenanceStatus status,
            LocalDate completedDate
    ){}

    public record MaintenanceResponse(
            Long id,
            Long assetId,
            String assetTag,
            String description,
            MaintenanceStatus status,
            LocalDate scheduledDate,
            LocalDate completedDate,
            String reportedBy
    ){}
}
