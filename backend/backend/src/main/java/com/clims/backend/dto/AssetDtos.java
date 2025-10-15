package com.clims.backend.dto;

import com.clims.backend.models.enums.AssetStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AssetDtos {
    public record CreateAssetRequest(
            @NotBlank String serialNumber,
            @NotBlank String make,
            @NotBlank String model,
            @NotNull LocalDate purchaseDate,
            LocalDate warrantyExpiryDate,
            Long locationId,
            Long vendorId,
            Long departmentId
    ){}

    public record UpdateAssetRequest(
            String make,
            String model,
            LocalDate warrantyExpiryDate,
            AssetStatus status,
            Long locationId,
            Long departmentId
    ){}

    public record AssignAssetRequest(
            @NotNull Long userId,
            Long locationId
    ){}

    public record AssetResponse(
            Long id,
            String assetTag,
            String serialNumber,
            String make,
            String model,
            AssetStatus status,
            String assignedTo,
            String location,
            String vendor,
            String department
    ){}
}
