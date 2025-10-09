package com.clims.backend.dto;

/** Lightweight projection for asset list views */
public interface AssetListProjection {
    Long getId();
    String getName();
    String getStatus();
    Long getAssignedUserId();
}
