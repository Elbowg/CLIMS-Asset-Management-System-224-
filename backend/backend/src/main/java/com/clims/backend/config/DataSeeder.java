package com.clims.backend.config;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.models.entities.Location;
import com.clims.backend.models.entities.Vendor;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.repositories.LocationRepository;
import com.clims.backend.repositories.VendorRepository;
import com.clims.backend.security.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(AppUserRepository users,
                               DepartmentRepository departments,
                               LocationRepository locations,
                               VendorRepository vendors,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Departments
            if (departments.findAll().isEmpty()) {
                Department it = new Department();
                it.setName("IT");
                departments.save(it);

                Department finance = new Department();
                finance.setName("Finance");
                departments.save(finance);
            }

            // Locations
            if (locations.findAll().isEmpty()) {
                Location hq = new Location();
                hq.setName("Headquarters");
                hq.setBuilding("A");
                hq.setRoom("101");
                locations.save(hq);

                Location store = new Location();
                store.setName("Store Room");
                store.setBuilding("B");
                store.setRoom("S1");
                locations.save(store);
            }

            // Vendors
            if (vendors.findAll().isEmpty()) {
                Vendor v = new Vendor();
                v.setName("Default Vendor");
                v.setEmail("vendor@example.com");
                vendors.save(v);
            }

            // Admin user
            if (!users.existsByUsername("admin")) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setEmail("admin@example.com");
                admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
                admin.setRole(Role.ADMIN);
                users.save(admin);
            }
        };
    }
}
