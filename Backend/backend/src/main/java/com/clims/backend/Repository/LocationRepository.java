package com.clims.backend.repository;

import com.clims.backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByRoomNumber(String roomNumber);
}