package com.clims.backend.services;

import com.clims.backend.models.entities.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarrantyScheduler {
    private static final Logger log = LoggerFactory.getLogger(WarrantyScheduler.class);
    private final AssetService assetService;

    public WarrantyScheduler(AssetService assetService) { this.assetService = assetService; }

    // Daily at 08:00
    @Scheduled(cron = "0 0 8 * * *")
    public void checkUpcomingExpirations() {
        List<Asset> expiring = assetService.upcomingWarrantyExpirations(30);
        if (!expiring.isEmpty()) {
            log.info("{} assets have warranties expiring within 30 days", expiring.size());
        }
    }
}
