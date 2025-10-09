package com.clims.perf;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Initial micro benchmark to establish a baseline for simple in-memory filtering & mapping
 * that approximates lightweight DTO projection for asset listing.
 *
 * This does NOT hit the Spring context or database; its purpose is to ensure JMH wiring works
 * and provide a placeholder for future DB-backed benchmarks (which may require testcontainers or
 * a dedicated harness outside pure JMH micro benchmarks).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class AssetListBenchmark {

    @State(Scope.Thread)
    public static class Data {
        List<DummyAsset> assets;

        @Setup
        public void setup() {
            assets = new ArrayList<>();
            for (int i = 0; i < 5_000; i++) {
                assets.add(new DummyAsset(i, "Asset-" + i, (i % 2 == 0) ? "ACTIVE" : "RETIRED", i % 10));
            }
        }
    }

    record DummyAsset(int id, String name, String status, int assignedUserId) {}
    record AssetProjection(int id, String name, String status, int assignedUserId) {}

    @Benchmark
    public List<AssetProjection> mapProjection(Data data) {
        List<AssetProjection> out = new ArrayList<>(data.assets.size());
        for (DummyAsset a : data.assets) {
            // Simulate projection logic (subset of columns)
            out.add(new AssetProjection(a.id(), a.name(), a.status(), a.assignedUserId()));
        }
        return out;
    }
}
