package com.clims.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@ConditionalOnClass(Caffeine.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(MeterRegistry meterRegistry) {
        // Asset cache: by id
        var caffeine = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .recordStats();

        var cache = new CaffeineCache("assetById", caffeine.build());
        // Micrometer ties into Spring Cache metrics automatically when micrometer-spring context is present
        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(cache));
        return manager;
    }
}
