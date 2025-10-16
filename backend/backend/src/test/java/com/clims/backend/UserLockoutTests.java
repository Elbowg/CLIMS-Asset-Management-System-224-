package com.clims.backend;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class UserLockoutTests {

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @Transactional
    public void failedAttempts_leadToLockAndUnlock_clearsLock() throws InterruptedException {
        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department(); d.setName("TestDept"); return departmentRepository.save(d);
        });

        AppUser u = new AppUser();
        u.setUsername("locktestuser");
        u.setEmail("locktest@example.com");
        u.setPasswordHash("noop");
        u.setRole(com.clims.backend.security.Role.EMPLOYEE);
        u.setDepartment(dept);
        userRepository.save(u);

        // simulate failed attempts up to max (using service defaults: 4 attempts, 3 minutes)
        int max = 4;
        long minutes = 3;
        for (int i = 0; i < max; i++) {
            userService.recordFailedLogin(u.getUsername(), max, minutes);
        }

        AppUser after = userRepository.findByUsername(u.getUsername()).orElseThrow();
        assertThat(userService.isLocked(after)).isTrue();

        // unlock and assert
        userService.unlockUser(after.getId());
        AppUser unlocked = userRepository.findByUsername(u.getUsername()).orElseThrow();
        assertThat(userService.isLocked(unlocked)).isFalse();
        assertThat(unlocked.getFailedLoginAttempts()).isEqualTo(0);
    }
}
