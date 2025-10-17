package com.clims.backend.dto;

import com.clims.backend.models.enums.AssetStatus;
import com.clims.backend.models.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AssetDtos {
    public record CreateAssetRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "serialNumber contains invalid characters") String serialNumber,
            @NotBlank String make,
            @NotBlank String model,
            @NotNull LocalDate purchaseDate,
            LocalDate warrantyExpiryDate,
            @NotNull(message = "type is required") AssetType type,
            Long locationId,
            Long vendorId,
            Long departmentId
    ){
    }

    public record UpdateAssetRequest(
            String make,
            String model,
            java.time.LocalDate warrantyExpiryDate,
            AssetStatus status,
            AssetType type,
            Long locationId,
            Long departmentId
    ){
    }

    public record AssignAssetRequest(
            @NotNull Long userId,
            Long locationId
    ){
    }

    public record AssetResponse(
            Long id,
            String assetTag,
            String serialNumber,
            String make,
            String model,
            AssetStatus status,
            AssetType type,
            String assignedTo,
            String location,
            String vendor,
            Long departmentId,
            String department
    ){
    }
}
