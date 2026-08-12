---
sidebar_position: 1
title: Performance & JMH Benchmarks
---

# Performance & JMH Benchmarks

TangoDB uses the **Java Microbenchmark Harness (JMH)** to empirically measure, analyze, and validate engine throughput, native allocation overheads, and disk serialization speeds.

---

## Benchmark Results Summary

The table below reports raw JMH microbenchmark throughput scores (`ops/s`) captured from synthetic benchmark runs in `benchmark_screenshot/v1`.

| Benchmark Class & Method | Parameter (`records`) | Mode | JMH Score (`ops/s`) | Error Margin | Benchmark Operation Scope |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`MemtablePutBenchmark.put`** | N/A | Throughput | **53,214,262.002** | ± 1,480,302.582 | 1 Single Key-Value Record Insert |
| **`SstManagerWriteBenchmark.write`** | 1,000 | Throughput | **550.999** | ± 158.142 | 1 Complete SSTable Flush (1,000 records) |
| **`SstManagerWriteBenchmark.write`** | 10,000 | Throughput | **358.586** | ± 75.964 | 1 Complete SSTable Flush (10,000 records) |
| **`SstManagerWriteBenchmark.write`** | 100,000 | Throughput | **84.219** | ± 6.657 | 1 Complete SSTable Flush (100,000 records) |

---

## 1. MemTable In-Memory Ingest Benchmark

`MemtablePutBenchmark.put` measures single-threaded concurrent key-value `put()` operations into off-heap `MemTable` memory segments managed via Java's Foreign Function & Memory (FFM) API (`MemorySegment` and `Arena`).

### Execution Details & Environment

* **JMH Harness Version**: 1.37
* **JVM Runtime**: OpenJDK 24 (64-Bit Server VM, Oracle Corp JDK 24+36-3646)
* **OS / Environment**: Windows AMD64
* **Warmup**: 5 iterations, 2 seconds each
* **Measurement**: 10 iterations, 3 seconds each across 2 forks

### Raw JMH Output

```text
Benchmark                  Mode  Cnt         Score         Error  Units
MemtablePutBenchmark.put  thrpt   18  53214262.002 ± 1480302.582  ops/s
```

* **JMH Score**: **53,214,262.002 ops/sec** (~53.21 Million single key-value record puts per second)
* **Minimum Iteration**: `49,016,247.392 ops/sec`
* **Maximum Iteration**: `56,032,506.943 ops/sec`
* **Standard Deviation**: `1,583,907.177 ops/sec`
* **Confidence Interval (99.9%)**: `[51,733,959.420, 54,694,564.583] ops/sec`

---

## 2. SSTable Disk Persistence Benchmark

`SstManagerWriteBenchmark.write` measures the performance of creating and persisting an entire SSTable data file to disk via `SstManager.write(immutableMemTable, directory)`.

> **Note on Benchmark Scope**: Unlike `MemtablePutBenchmark` where 1 operation corresponds to 1 record insert, 1 operation (`op`) in `SstManagerWriteBenchmark` corresponds to **flushing an entire frozen `ImmutableMemTable` containing N records** to disk.

### Raw JMH Output

```text
Benchmark                       (records)   Mode  Cnt    Score     Error  Units
SstManagerWriteBenchmark.write       1000  thrpt   10  550.999 ± 158.142  ops/s
SstManagerWriteBenchmark.write      10000  thrpt   10  358.586 ±  75.964  ops/s
SstManagerWriteBenchmark.write     100000  thrpt   10   84.219 ±   6.657  ops/s
```

### Interpretation of Results

* **1,000 Records per MemTable**: Achieves **550.999 full SSTable file flushes/sec**.
* **10,000 Records per MemTable**: Achieves **358.586 full SSTable file flushes/sec**.
* **100,000 Records per MemTable**: Achieves **84.219 full SSTable file flushes/sec**.

---

## The Optimization Journey

```text
  Phase 1: Heap Allocations        Phase 2: FFM Native Segments      Phase 3: Contiguous Arenas
┌──────────────────────────┐    ┌──────────────────────────┐    ┌──────────────────────────┐
│ On-heap allocations      │───►│ Move record payload      │───►│ Pre-allocate 64 MiB      │
│ induced GC pauses under  │    │ off-heap via FFM         │    │ Arena blocks & lock-free │
│ high write throughput.   │    │ MemorySegments.          │    │ SkipList indexing.       │
└──────────────────────────┘    └──────────────────────────┘    └──────────────────────────┘
                                                                             │
                                                                             ▼
                                                                  53.2M+ ops/sec MemTable Put
                                                                  84.2 ops/sec 100K SSTable Flush
```

### Architectural Performance Drivers

1. **Reduced GC Pressure**: Record payloads are stored off-heap in native `MemorySegment`s, preventing JVM heap allocation spikes during heavy ingest.
2. **Lock-Free Indexing**: The `ConcurrentSkipListMap` index maintains key-to-offset primitive pointers without thread contention.
3. **Contiguous Native Memory Serialization**: Binary record blocks stream directly from native memory to disk channels during background flushing.
