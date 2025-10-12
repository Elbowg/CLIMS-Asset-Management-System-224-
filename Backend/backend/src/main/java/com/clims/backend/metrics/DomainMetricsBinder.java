package com.clims.backend.metrics;

import com.clims.backend.model.AssetStatus;
import com.clims.backend.repository.AssetRepository;
import com.clims.backend.repository.MaintenanceRepository;
import com.clims.backend.security.TokenBlacklistService; // concrete used for size (interface has no size method)
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publishes periodically refreshed gauges for domain KPIs: asset counts by status, maintenance total, blacklist size.
 */
@Configuration
@ConditionalOnBean(MeterRegistry.class)
@Profile("!insecure") // Don't load in insecure profile
public class DomainMetricsBinder {

    private final AssetRepository assetRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final MeterRegistry registry;
    private final TokenBlacklistService blacklistService;

    private final Map<AssetStatus, AtomicLong> assetStatusGauges = new EnumMap<>(AssetStatus.class);
    private final AtomicLong maintenanceTotal = new AtomicLong();
    private final AtomicLong blacklistSize = new AtomicLong();

    public DomainMetricsBinder(AssetRepository assetRepository,
                               MaintenanceRepository maintenanceRepository,
                               MeterRegistry registry,
                               TokenBlacklistService blacklistService) {
        this.assetRepository = assetRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.registry = registry;
        this.blacklistService = blacklistService;
    }

    @PostConstruct
    void init() {
        for (AssetStatus status : AssetStatus.values()) {
            AtomicLong holder = new AtomicLong(0);
            assetStatusGauges.put(status, holder);
            Gauge.builder("domain.assets.count", holder, AtomicLong::get)
                    .description("Number of assets in a given status")
                    .tag("status", status.name().toLowerCase())
                    .register(registry);
        }
        Gauge.builder("domain.maintenance.total", maintenanceTotal, AtomicLong::get)
                .description("Total maintenance records")
                .register(registry);
        Gauge.builder("auth.blacklist.size", blacklistSize, AtomicLong::get)
                .description("Current JWT blacklist size (in-memory)")
                .register(registry);
        refresh();
    }

    @Scheduled(fixedDelayString = "PT30S")
    void refresh() {
        assetStatusGauges.values().forEach(g -> g.set(0));
        assetRepository.findAll().forEach(a -> assetStatusGauges.get(a.getStatus()).incrementAndGet());
        maintenanceTotal.set(maintenanceRepository.count());
        // Reflection to peek at map size (simplistic) since no interface method; acceptable for internal metrics
        try {
            var field = TokenBlacklistService.class.getDeclaredField("blacklist");
            field.setAccessible(true);
            Object map = field.get(blacklistService);
            int size = (map instanceof java.util.Map<?,?> m) ? m.size() : -1;
            blacklistSize.set(size);
        } catch (Exception ignored) { }
    }
}
