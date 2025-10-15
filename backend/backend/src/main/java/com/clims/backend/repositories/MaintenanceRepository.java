package com.clims.backend.repositories;

import com.clims.backend.models.entities.Maintenance;
import com.clims.backend.models.enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MaintenanceRepository extends JpaRepository<Maintenance, Long>, JpaSpecificationExecutor<Maintenance> {
    List<Maintenance> findByStatus(MaintenanceStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM Maintenance m WHERE m.scheduledDate >= :from AND m.status <> com.clims.backend.models.enums.MaintenanceStatus.COMPLETED")
    long countUpcomingFrom(@org.springframework.data.repository.query.Param("from") java.time.LocalDate from);
}
