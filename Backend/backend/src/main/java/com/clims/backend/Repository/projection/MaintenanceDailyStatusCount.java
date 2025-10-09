package com.clims.backend.repository.projection;

import com.clims.backend.model.MaintenanceStatus;
import java.time.LocalDate;

public interface MaintenanceDailyStatusCount {
    LocalDate getDay();
    MaintenanceStatus getStatus();
    long getCount();
}
