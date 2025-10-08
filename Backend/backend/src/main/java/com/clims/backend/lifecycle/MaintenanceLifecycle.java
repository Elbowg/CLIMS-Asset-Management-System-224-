package com.clims.backend.lifecycle;

import com.clims.backend.exception.BusinessRuleException;
import com.clims.backend.model.MaintenanceStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Centralized maintenance status graph. */
public final class MaintenanceLifecycle {
    private static final Map<MaintenanceStatus, Set<MaintenanceStatus>> GRAPH = new EnumMap<>(MaintenanceStatus.class);
    static {
        GRAPH.put(MaintenanceStatus.REPORTED, EnumSet.of(MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.CANCELLED));
        GRAPH.put(MaintenanceStatus.IN_PROGRESS, EnumSet.of(MaintenanceStatus.RESOLVED, MaintenanceStatus.CANCELLED));
        GRAPH.put(MaintenanceStatus.RESOLVED, EnumSet.noneOf(MaintenanceStatus.class));
        GRAPH.put(MaintenanceStatus.CANCELLED, EnumSet.noneOf(MaintenanceStatus.class));
    }

    private MaintenanceLifecycle() {}

    public static void validateTransition(MaintenanceStatus from, MaintenanceStatus to) {
        if (from == null) return; // initial
        Set<MaintenanceStatus> allowed = GRAPH.getOrDefault(from, EnumSet.noneOf(MaintenanceStatus.class));
        if (!allowed.contains(to)) {
            throw new BusinessRuleException("Invalid maintenance status transition: " + from + " -> " + to);
        }
    }
}
