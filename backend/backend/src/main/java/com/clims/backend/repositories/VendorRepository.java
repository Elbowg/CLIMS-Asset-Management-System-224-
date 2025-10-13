package com.clims.backend.repositories;

import com.clims.backend.models.entities.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> { }
