package com.clims.backend.repositories;

import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long> {
    List<Maintenance> findByStatus(MaintenanceStatus status);
}
