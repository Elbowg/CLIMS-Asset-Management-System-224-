package com.clims.backend.model.summary;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "maintenance_activity_daily")
public class MaintenanceActivityDaily {

    @EmbeddedId
    private MaintenanceActivityDailyId id;

    @Column(name = "activity_count", nullable = false)
    private long activityCount;

    public MaintenanceActivityDaily() {}

    public MaintenanceActivityDaily(MaintenanceActivityDailyId id, long activityCount) {
        this.id = id;
        this.activityCount = activityCount;
    }

    public MaintenanceActivityDailyId getId() {
        return id;
    }

    public void setId(MaintenanceActivityDailyId id) {
        this.id = id;
    }

    public long getActivityCount() {
        return activityCount;
    }

    public void setActivityCount(long activityCount) {
        this.activityCount = activityCount;
    }
}
