package com.clims.backend.model.summary;

import com.clims.backend.model.MaintenanceStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class MaintenanceActivityDailyId implements Serializable {

    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_status", nullable = false, length = 32)
    private MaintenanceStatus maintenanceStatus;

    public MaintenanceActivityDailyId() {}

    public MaintenanceActivityDailyId(LocalDate bucketDate, MaintenanceStatus maintenanceStatus) {
        this.bucketDate = bucketDate;
        this.maintenanceStatus = maintenanceStatus;
    }

    public LocalDate getBucketDate() {
        return bucketDate;
    }

    public void setBucketDate(LocalDate bucketDate) {
        this.bucketDate = bucketDate;
    }

    public MaintenanceStatus getMaintenanceStatus() {
        return maintenanceStatus;
    }

    public void setMaintenanceStatus(MaintenanceStatus maintenanceStatus) {
        this.maintenanceStatus = maintenanceStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaintenanceActivityDailyId that = (MaintenanceActivityDailyId) o;
        return Objects.equals(bucketDate, that.bucketDate) && maintenanceStatus == that.maintenanceStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketDate, maintenanceStatus);
    }
}
