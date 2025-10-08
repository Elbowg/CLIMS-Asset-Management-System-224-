package com.clims.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TempHashPrinterTest {
    @Test
    void printUserHash() {
        var enc = new BCryptPasswordEncoder();
        String hash = enc.encode("user");
        System.out.println("BCrypt hash for 'user': " + hash);
    }
}
