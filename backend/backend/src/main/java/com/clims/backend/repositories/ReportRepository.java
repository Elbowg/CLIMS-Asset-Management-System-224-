package com.clims.backend.repositories;

import com.clims.backend.models.entities.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> { }
