---
sidebar_position: 1
title: Introduction & Status
slug: /intro
---

# TangoDB Overview & Status

**TangoDB** is an **experimental high-performance embedded key-value storage engine built from scratch in modern Java**.

The project explores how modern JVM capabilities — specifically the **Foreign Function & Memory (FFM) API**, **off-heap memory management**, and **lock-free concurrent data structures** — can be leveraged to build a storage engine designed for **extreme throughput, minimal GC latency, concurrent workloads, and predictable memory usage**.

:::warning[EXPERIMENTAL STATUS & STABILITY DISCLAIMER]
**TangoDB V1 is experimental in implementation maturity.** 

Public APIs, configuration structures, internal byte formats, and SSTable disk layouts are **subject to change without backward compatibility guarantees**.
:::

---

## Core Mission & Engineering Question

TangoDB started with a central engineering question:

> **How far can modern Java be pushed when building a high-performance storage engine?**

Instead of relying on legacy Java object allocation patterns that induce Garbage Collection pauses under heavy write loads, TangoDB embraces:

1. **Off-Heap Native Allocation**: Storing raw key-value record payloads off-heap using Java's Foreign Function & Memory (FFM) API (`java.lang.foreign.MemorySegment` and `Arena`).
2. **Lock-Free Concurrent Indexing**: Maintaining an in-memory skip list index (`ConcurrentSkipListMap`) mapping keys to native off-heap memory offsets.
3. **LSM-Tree Storage Pipeline**: Log-Structured Merge-Tree storage model featuring concurrent active `MemTable`s, immutable frozen `MemTable`s, asynchronous background flushing (`FlushManager`), and background compaction (`CompactionManager`).
4. **Fast Disk Persistence**: Immutable SSTables indexed by page metadata headers and mapped directly into native memory via `MemorySegment.ofBuffer(MappedByteBuffer)`.

---

## Performance Baseline Highlights

In JMH (Java Microbenchmark Harness) microbenchmarks, TangoDB achieves:

* **In-Memory MemTable Ingest**: **53,214,262 ops/sec** (~53.2 Million key-value `put()` calls per second).
* **SSTable Disk Flushes**: **84.219 ops/sec** for full 100,000-record SSTable file writes to disk.
* **Reduced GC Pressure**: Storing key-value record payloads off-heap significantly reduces JVM Garbage Collection overhead and pause times under heavy write workloads.

```text
Benchmark                       (records)   Mode  Cnt         Score     Error  Units
MemtablePutBenchmark.put              N/A  thrpt   18  53214262.002 ± 1480302  ops/s
SstManagerWriteBenchmark.write     100000  thrpt   10        84.219 ±     6.6  ops/s (Table Flushes/s)
```

*Explore the complete design breakdown, optimization journey, and raw benchmark scores in [Performance & Benchmarks](/docs/benchmarks/performance).*
