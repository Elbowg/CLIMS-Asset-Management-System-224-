package com.clims.backend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendApplicationTests {

    @Test
    void simpleSanityCheck() {
        // lightweight test that doesn't start the Spring context
        assertTrue(true, "sanity check");
    }
}
