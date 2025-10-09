package com.clims.backend.service;

import com.clims.backend.model.Asset;
import com.clims.backend.model.AssetStatus;
import com.clims.backend.model.User;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.AssignmentHistoryRepository;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AssetServiceCachingTest.TestConfig.class)
@ActiveProfiles("test")
class AssetServiceCachingTest {

    @Configuration
    @Import(AssetService.class)
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            CaffeineCacheManager m = new CaffeineCacheManager("assetById");
            m.setCaffeine(Caffeine.newBuilder().maximumSize(1000));
            return m;
        }

        @Bean
        @Primary
        AssetRepository assetRepository() {
            return Mockito.mock(AssetRepository.class);
        }

        @Bean
        @Primary
        AssignmentHistoryRepository assignmentHistoryRepository() {
            return Mockito.mock(AssignmentHistoryRepository.class);
        }

        @Bean
        @Primary
        OutboxEventService outboxEventService() {
            return Mockito.mock(OutboxEventService.class);
        }
    }

    @Autowired
    AssetService assetService; // real bean with caching proxy

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssignmentHistoryRepository historyRepository;

    @Autowired
    OutboxEventService outboxEventService; // prevent side-effects in create()

    private Asset makeAsset(long id, String name, AssetStatus status) {
        Asset a = new Asset();
        a.setId(id);
        a.setName(name);
        a.setStatus(status);
        return a;
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(assetRepository, historyRepository, outboxEventService);
    }

    @Test
    void getByIdOrThrow_usesCache_onSecondCall() {
        Asset a = makeAsset(42L, "Laptop", AssetStatus.AVAILABLE);
        when(assetRepository.findById(42L)).thenReturn(Optional.of(a));

        // First call loads from repo
        Asset first = assetService.getByIdOrThrow(42L);
        assertEquals(42L, first.getId());

        // Second call should be served by cache
        Asset second = assetService.getByIdOrThrow(42L);
        assertEquals(42L, second.getId());

        verify(assetRepository, times(1)).findById(42L);
    }

    @Test
    void create_putsEntity_inCache() {
        Asset toCreate = makeAsset(0L, "New", AssetStatus.AVAILABLE);
        // Simulate save assigning an ID
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> {
            Asset saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        Asset created = assetService.create(toCreate);
        assertEquals(100L, created.getId());

        // After create, entity should be in cache. A get should NOT hit repository
        reset(assetRepository);
        Asset cached = assetService.getByIdOrThrow(100L);
        assertEquals(100L, cached.getId());
        verify(assetRepository, times(0)).findById(100L);
    }

    @Test
    void update_evictsCache_entry() {
        long id = 7L;
        Asset original = makeAsset(id, "Original", AssetStatus.AVAILABLE);
        when(assetRepository.findById(id)).thenReturn(Optional.of(original));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        // Prime cache
        assetService.getByIdOrThrow(id);
        verify(assetRepository, times(1)).findById(id);

        // Update should evict cache
        reset(assetRepository);
        when(assetRepository.findById(id)).thenReturn(Optional.of(original));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Asset updatedPayload = makeAsset(id, "Updated", AssetStatus.AVAILABLE);
        assetService.update(id, updatedPayload);

    // Fetch again should hit repository because cache was evicted
    assetService.getByIdOrThrow(id);
    verify(assetRepository, times(2)).findById(id);
    }

    @Test
    void delete_evictsCache_entry() {
        long id = 9L;
        Asset a = makeAsset(id, "ToDelete", AssetStatus.AVAILABLE);
        when(assetRepository.findById(id)).thenReturn(Optional.of(a));

        // Prime cache
        assetService.getByIdOrThrow(id);
        verify(assetRepository, times(1)).findById(id);

        // Delete should evict
        reset(assetRepository);
        doNothing().when(assetRepository).deleteById(id);
        when(assetRepository.findById(id)).thenReturn(Optional.of(a));
        assetService.deleteById(id);

        // Fetch again should hit repository due to eviction
        assetService.getByIdOrThrow(id);
        verify(assetRepository, times(1)).findById(id);
    }

    @Test
    void assignToUser_updatesCache_entry() {
        long id = 11L;
        Asset a = makeAsset(id, "Asset", AssetStatus.AVAILABLE);
        when(assetRepository.findById(id)).thenReturn(Optional.of(a));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        // Prime cache
        assetService.getByIdOrThrow(id);
        verify(assetRepository, times(1)).findById(id);

        // Assign should update cache via @CachePut
        User u = new User();
        u.setId(5L);

        reset(assetRepository);
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assetRepository.findById(id)).thenReturn(Optional.of(a)); // in case service path re-reads
        assetService.assignToUser(a, u);

        // Next get should use cache (no repo hit) and reflect ASSIGNED status
        Asset cached = assetService.getByIdOrThrow(id);
        assertEquals(AssetStatus.ASSIGNED, cached.getStatus());
        verify(assetRepository, times(0)).findById(id);
    }
}
