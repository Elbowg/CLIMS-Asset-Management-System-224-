package com.clims.backend.controllers;

import com.clims.backend.models.entities.Department;
import com.clims.backend.models.entities.Location;
import com.clims.backend.models.entities.Vendor;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.repositories.LocationRepository;
import com.clims.backend.repositories.VendorRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lookups")
public class LookupController {
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final VendorRepository vendorRepository;

    public LookupController(DepartmentRepository departmentRepository, LocationRepository locationRepository, VendorRepository vendorRepository) {
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.vendorRepository = vendorRepository;
    }

    @GetMapping("/departments")
    public List<Department> departments() { return departmentRepository.findAll(); }

    @GetMapping("/locations")
    public List<Location> locations() { return locationRepository.findAll(); }

    @GetMapping("/vendors")
    public List<Vendor> vendors() { return vendorRepository.findAll(); }
}
