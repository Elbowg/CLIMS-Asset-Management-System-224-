# Performance (JMH) Module

This module houses micro benchmarks used to establish and track performance baselines as optimization work progresses (Phase 4).

## Activation
The module is excluded from the default build to keep CI fast. Activate it explicitly:

```
mvn -Pperf clean package
```

## Running Benchmarks
After packaging (or directly via exec):

```
# Fat JAR with shaded JMH entry point
java -jar perf/target/perf.jar -f 1 -wi 3 -i 5
```

Common flags:
- `-f` forks (1-2 typical; increase for stable results)
- `-wi` warmup iterations
- `-i` measurement iterations
- `-bm` benchmark mode (defaults set via annotations)
- `-prof gc` add GC profiling

## Current Benchmarks
| Benchmark | Purpose |
|-----------|---------|
| `AssetListBenchmark.mapProjection` | Baseline cost of mapping a simple list of 5k in-memory domain objects to lightweight projection records. |

## Roadmap
Planned future additions (tracked in Phase 4 epics):
1. Repository-level projection vs entity materialization comparison.
2. Pagination path benchmarks with varying page sizes (e.g., 20/50/100/500).
3. JPA vs custom SQL read performance.
4. Allocation profiling under parallel streams / mapstruct usage.
5. Caffeine cache hit/miss cost modeling (post cache introduction).

## Methodology Notes
- Micro benchmarks isolate CPU + allocation micro costs; they intentionally avoid network / DB variability.
- For end-to-end latency & throughput, use a separate load harness (Gatling / k6 / Locust) outside JMH.
- Always pin Java version & note exact commit when capturing a baseline.

## Reproducibility Checklist
1. Close other heavy processes (reduce noise).
2. Use performance power plan (on laptops).
3. Run with `-Xms2g -Xmx2g` (fixed heap) for reduced GC variance, e.g.:
   ```
   java -Xms2g -Xmx2g -jar perf/target/perf.jar -f 2 -wi 5 -i 10
   ```
4. Capture environment: `java -version`, OS, CPU model.
5. Store raw JMH output (append to `Docs/perf/baselines/`).

## Baseline Capture Guide
When first introducing a performance-affecting change (e.g., pagination projection), run benchmarks on main before the change and again after merging. Record delta (% change). Aim for ±5% noise band; repeat if higher variance.

---
Generated initial skeleton as part of Phase 4 kickoff.

## Baselines

Quick smoke baseline captured on 2025-10-09 (JDK 23), command parameters: `-f 1 -wi 1 -i 3 -r 500ms -bm avgt -tu ms`.

- AssetListBenchmark.mapProjection: ~0.053 ms/op (avg), see `Backend/perf/target/jmh-results-2025-10-09.json` for the full JSON.

Notes:
- This is a short-run sanity check; for authoritative baselines, increase warmups, iterations, and forks (see “Reproducibility Checklist”).
