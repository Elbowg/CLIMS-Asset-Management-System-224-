package com.clims.backend.testutil;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Utility assertion helpers for tests. */
public final class TestAssertions {
    private TestAssertions() {}

    /** Assert the given string is a valid canonical UUID representation. */
    public static void assertUuid(String value) {
        assertThat(value).isNotBlank();
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new AssertionError("Expected a UUID but was: " + value, ex);
        }
    }
}
