package com.clims.backend.service;

import com.clims.backend.exception.ResourceNotFoundException;
import com.clims.backend.model.Vendor;
import com.clims.backend.Repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class VendorService {

    private final VendorRepository repo;

    public VendorService(VendorRepository repo) { this.repo = repo; }

    public List<Vendor> findAll() { return repo.findAll(); }

    public Optional<Vendor> findById(Long id) { return repo.findById(id); }

    public Vendor getByIdOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Vendor", id));
    }

    public Vendor create(Vendor vendor) {
        vendor.setId(null);
        return repo.save(vendor);
    }

    public Optional<Vendor> update(Long id, Vendor updated) {
        return repo.findById(id).map(existing -> {
            existing.setVendorName(updated.getVendorName());
            existing.setContactInfo(updated.getContactInfo());
            return repo.save(existing);
        });
    }

    public void delete(Long id) { repo.deleteById(id); }
}

