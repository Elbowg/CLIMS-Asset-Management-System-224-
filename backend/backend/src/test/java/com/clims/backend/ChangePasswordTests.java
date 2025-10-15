package com.clims.backend;

import com.clims.backend.models.entities.AppUser;
import com.clims.backend.repositories.AppUserRepository;
import com.clims.backend.repositories.DepartmentRepository;
import com.clims.backend.services.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.modelmapper.ModelMapper;

import static org.mockito.ArgumentMatchers.any;

public class ChangePasswordTests {

    @Test
    void changePassword_success_updatesHash() {
        AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
        DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        AppUser user = new AppUser();
        user.setId(100L);
        user.setPasswordHash("oldHash");

        Mockito.when(userRepo.findById(100L)).thenReturn(java.util.Optional.of(user));
        Mockito.when(encoder.matches("current", "oldHash")).thenReturn(true);
    Mockito.when(encoder.encode("Newpass1!")).thenReturn("newHash");
        Mockito.when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

    UserService svc = new UserService(userRepo, deptRepo, encoder, new ModelMapper());
    svc.changePassword(100L, "current", "Newpass1!");

        Mockito.verify(userRepo).save(Mockito.argThat(u -> "newHash".equals(u.getPasswordHash())));
    }

    @Test
    void changePassword_wrongCurrent_throws() {
        AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
        DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        AppUser user = new AppUser();
        user.setId(101L);
        user.setPasswordHash("oldHash");

        Mockito.when(userRepo.findById(101L)).thenReturn(java.util.Optional.of(user));
        Mockito.when(encoder.matches("wrong", "oldHash")).thenReturn(false);

        UserService svc = new UserService(userRepo, deptRepo, encoder, new ModelMapper());

        Assertions.assertThrows(IllegalArgumentException.class, () -> svc.changePassword(101L, "wrong", "x"));
    }

    @Test
    void changePassword_weakPassword_throws() {
        AppUserRepository userRepo = Mockito.mock(AppUserRepository.class);
        DepartmentRepository deptRepo = Mockito.mock(DepartmentRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);

        AppUser user = new AppUser();
        user.setId(102L);
        user.setPasswordHash("oldHash");

        Mockito.when(userRepo.findById(102L)).thenReturn(java.util.Optional.of(user));
        Mockito.when(encoder.matches("current", "oldHash")).thenReturn(true);

        UserService svc = new UserService(userRepo, deptRepo, encoder, new ModelMapper());

        Assertions.assertThrows(IllegalArgumentException.class, () -> svc.changePassword(102L, "current", "weak"));
    }
}
