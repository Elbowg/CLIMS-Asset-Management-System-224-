package com.clims.backend.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesValidationTest {

    @Test
    void logicalConsistencyCheckFailsWhenRefreshLessThanAccess() {
        JwtProperties props = new JwtProperties();
        props.setSecret("01234567890123456789012345678901XXXX");
        props.setAccessExpiration(120_000);
        props.setRefreshExpiration(60_000); // invalid
        assertThatThrownBy(() -> {
            // manually invoke lifecycle validation
            props.setRefreshExpiration(60_000);
            // call private method via reflection since @PostConstruct not triggered
            var m = JwtProperties.class.getDeclaredMethod("validateLogicalConsistency");
            m.setAccessible(true);
            m.invoke(props);
        }).hasCauseInstanceOf(IllegalStateException.class);
    }
}
