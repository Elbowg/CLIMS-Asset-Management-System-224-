package com.clims.backend.lifecycle;

import com.clims.backend.exception.BusinessRuleException;
import com.clims.backend.model.AssetStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Centralizes allowed asset status transitions. */
public final class AssetLifecycle {
    private static final Map<AssetStatus, Set<AssetStatus>> GRAPH = new EnumMap<>(AssetStatus.class);
    static {
        GRAPH.put(AssetStatus.AVAILABLE, EnumSet.of(AssetStatus.ASSIGNED, AssetStatus.MAINTENANCE, AssetStatus.RETIRED));
        GRAPH.put(AssetStatus.ASSIGNED, EnumSet.of(AssetStatus.AVAILABLE, AssetStatus.MAINTENANCE, AssetStatus.RETIRED));
        GRAPH.put(AssetStatus.MAINTENANCE, EnumSet.of(AssetStatus.AVAILABLE, AssetStatus.RETIRED));
        GRAPH.put(AssetStatus.RETIRED, EnumSet.noneOf(AssetStatus.class));
    }

    private AssetLifecycle() {}

    public static void validateTransition(AssetStatus from, AssetStatus to) {
        if (from == null) return; // initial creation
        Set<AssetStatus> allowed = GRAPH.getOrDefault(from, EnumSet.noneOf(AssetStatus.class));
        if (!allowed.contains(to)) {
            throw new BusinessRuleException("Invalid asset status transition: " + from + " -> " + to);
        }
    }
}
