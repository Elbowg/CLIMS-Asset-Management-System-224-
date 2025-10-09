package com.clims.backend.model.summary;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_status_daily")
public class AssetStatusDaily {

    @EmbeddedId
    private AssetStatusDailyId id;

    @Column(name = "asset_count", nullable = false)
    private long assetCount;

    public AssetStatusDaily() {}

    public AssetStatusDaily(AssetStatusDailyId id, long assetCount) {
        this.id = id;
        this.assetCount = assetCount;
    }

    public AssetStatusDailyId getId() { return id; }
    public void setId(AssetStatusDailyId id) { this.id = id; }
    public long getAssetCount() { return assetCount; }
    public void setAssetCount(long assetCount) { this.assetCount = assetCount; }
}
