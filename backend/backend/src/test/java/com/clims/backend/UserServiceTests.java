package com.clims.backend;

import com.clims.backend.dto.UserDtos;
import com.clims.backend.exceptions.NotFoundException;
import com.clims.backend.models.entities.AppUser;
import com.clims.backend.models.entities.Department;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.security.Role;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class UserServiceTests {

    @Test
    void register_setsPassword_andSaves() {
        AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
        DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        Mockito.when(encoder.encode("secret")).thenReturn("enc");
        Mockito.when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        UserService svc = new UserService(userRepo, deptRepo, encoder, new ModelMapper());
    UserDtos.RegisterRequest req = new UserDtos.RegisterRequest("alice", "a@x.com", "secret", Role.EMPLOYEE, null);

        AppUser saved = svc.register(req);
        Assertions.assertEquals(42L, saved.getId());
        Assertions.assertEquals("enc", saved.getPasswordHash());
    Assertions.assertEquals(Role.EMPLOYEE, saved.getRole());
    }

    @Test
    void register_withMissingDept_throwsNotFound() {
        UserService svc = new UserService(
                Mockito.mock(AppUserRepository.class),
                Mockito.mock(DepartmentRepository.class),
                Mockito.mock(PasswordEncoder.class),
                new ModelMapper()
        );
    UserDtos.RegisterRequest req = new UserDtos.RegisterRequest("bob", "b@x.com", "p", Role.EMPLOYEE, 999L);
        Assertions.assertThrows(NotFoundException.class, () -> svc.register(req));
    }

    @Test
    void register_withDept_setsDepartment() {
        AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
        DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        Department dept = new Department();
        dept.setId(5L);
        dept.setName("IT");
        Mockito.when(deptRepo.findById(5L)).thenReturn(Optional.of(dept));
        Mockito.when(encoder.encode("pw")).thenReturn("hashed");
        Mockito.when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService svc = new UserService(userRepo, deptRepo, encoder, new ModelMapper());
        UserDtos.RegisterRequest req = new UserDtos.RegisterRequest("tom", "t@x.com", "pw", Role.IT_STAFF, 5L);

        AppUser saved = svc.register(req);
        Assertions.assertNotNull(saved.getDepartment());
        Assertions.assertEquals(5L, saved.getDepartment().getId());
        Assertions.assertEquals("hashed", saved.getPasswordHash());
    }
}
