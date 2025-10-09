package com.clims.backend.repository.projection;

import java.time.LocalDate;

public interface AuditDailyActionCount {
    LocalDate getDay();
    String getAction();
    long getCount();
}
