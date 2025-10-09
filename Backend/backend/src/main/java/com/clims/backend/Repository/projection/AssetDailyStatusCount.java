package com.clims.backend.repository.projection;

import com.clims.backend.model.AssetStatus;
import java.time.LocalDate;

public interface AssetDailyStatusCount {
    LocalDate getDay();
    AssetStatus getStatus();
    long getCount();
}
