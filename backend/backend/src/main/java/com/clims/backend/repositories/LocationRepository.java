package com.clims.backend.repositories;

import com.clims.backend.models.entities.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> { }
