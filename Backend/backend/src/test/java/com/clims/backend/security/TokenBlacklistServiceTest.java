package com.clims.backend.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistServiceTest {
    @Test
    void blacklistAndExpiry() throws InterruptedException {
        TokenBlacklistService svc = new TokenBlacklistService();
        svc.blacklist("abc", 1); // 1s
        assertThat(svc.isBlacklisted("abc")).isTrue();
        Thread.sleep(1100);
        assertThat(svc.isBlacklisted("abc")).isFalse();
    }
}