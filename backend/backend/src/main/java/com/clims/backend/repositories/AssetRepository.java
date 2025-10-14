package com.clims.backend.repositories;

import com.clims.backend.models.entities.Asset;
import com.clims.backend.models.enums.AssetStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    Optional<Asset> findByAssetTag(String assetTag);
    List<Asset> findByStatus(AssetStatus status);
    List<Asset> findByWarrantyExpiryDateBefore(LocalDate date);
}
