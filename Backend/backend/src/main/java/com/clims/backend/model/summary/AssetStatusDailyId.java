package com.clims.backend.model.summary;

import com.clims.backend.model.AssetStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class AssetStatusDailyId implements Serializable {
    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status", nullable = false, length = 32)
    private AssetStatus assetStatus;

    public AssetStatusDailyId() {}

    public AssetStatusDailyId(LocalDate bucketDate, AssetStatus assetStatus) {
        this.bucketDate = bucketDate;
        this.assetStatus = assetStatus;
    }

    public LocalDate getBucketDate() { return bucketDate; }
    public void setBucketDate(LocalDate bucketDate) { this.bucketDate = bucketDate; }
    public AssetStatus getAssetStatus() { return assetStatus; }
    public void setAssetStatus(AssetStatus assetStatus) { this.assetStatus = assetStatus; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssetStatusDailyId that = (AssetStatusDailyId) o;
        return Objects.equals(bucketDate, that.bucketDate) && assetStatus == that.assetStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketDate, assetStatus);
    }
}
