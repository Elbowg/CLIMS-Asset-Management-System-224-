package com.clims.backend.config;

import com.clims.backend.model.Role;
import com.clims.backend.model.User;
import com.clims.backend.repository.RoleRepository;
import com.clims.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Data initializer for the insecure profile.
 * Seeds demo users (admin/admin, user/user) for development.
 */
@Configuration
@Profile("insecure")
public class InsecureDataInitializer {
    
    private static final Logger log = LoggerFactory.getLogger(InsecureDataInitializer.class);
    
    @Bean
    public CommandLineRunner seedDemoUsers(UserRepository userRepository, 
                                          RoleRepository roleRepository,
                                          PasswordEncoder passwordEncoder) {
        return args -> {
            // Create roles if they don't exist
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_ADMIN");
                    return roleRepository.save(role);
                });
            
            Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName("ROLE_USER");
                    return roleRepository.save(role);
                });
            
            // Create admin user if doesn't exist
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setFullName("Admin User");
                admin.setEmail("admin@clims.local");
                admin.setRoles(Set.of(adminRole));
                userRepository.save(admin);
                log.info("✅ Created demo admin user: admin/admin");
            }
            
            // Create regular user if doesn't exist
            if (userRepository.findByUsername("user").isEmpty()) {
                User user = new User();
                user.setUsername("user");
                user.setPassword(passwordEncoder.encode("user"));
                user.setFullName("Regular User");
                user.setEmail("user@clims.local");
                user.setRoles(Set.of(userRole));
                userRepository.save(user);
                log.info("✅ Created demo regular user: user/user");
            }
            
            log.info("✅ Insecure profile data initialization complete");
        };
    }
}
