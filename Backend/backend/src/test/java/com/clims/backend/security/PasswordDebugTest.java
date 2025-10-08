package com.clims.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Temporary test to output BCrypt hash for 'admin' so we can compare with seeded value. */
public class PasswordDebugTest {

    @Test
    void printHash() {
        PasswordEncoder enc = new BCryptPasswordEncoder();
        String hash = enc.encode("admin");
        System.out.println("BCrypt hash for 'admin': " + hash);
    }
}
