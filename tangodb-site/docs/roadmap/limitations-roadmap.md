---
sidebar_position: 1
title: Limitations & Future Roadmap
---

# Limitations & Future Roadmap

TangoDB V1 is an experimental storage engine baseline focused on high-throughput in-memory ingest and LSM-tree persistence primitives.

---

## Current V1 Limitations

1. **Experimental API & Storage Formats**:
   Public Java APIs, configuration interfaces, native memory binary layouts, and SSTable disk file formats are actively evolving and subject to non-backward-compatible changes.

2. **No Write-Ahead Log (WAL) for Power-Fault Durability**:
   In V1, records written to the active `MemTable` reside in native off-heap memory before being flushed to disk as SSTables. A sudden power loss or process kill before flushing will lose un-flushed `MemTable` records.

3. **Single-Level SSTable Merging**:
   The current `CompactionManager` executes basic multi-way merge compaction across flushed SSTables rather than a full multi-tiered Leveled Compaction tree (L0..Lmax).

4. **Off-Heap Memory Sizing Bounds**:
   Native memory allocated via FFM `Arena` blocks bypasses standard JVM `-Xmx` heap limits and requires OS native memory availability.

---

## Future Engineering Roadmap

```text
 ┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
 │ Bloom Filter         │ ──►│ Write-Ahead Log (WAL)│ ──►│ Leveled Compaction   │
 │ SSTable Integration  │    │ Crash Recovery       │    │ Tiering (L0..Lmax)   │
 └──────────────────────┘    └──────────────────────┘    └──────────────────────┘
```

### 1. Bloom Filter Integration & Disk Lookups
Serialize Bloom filter bitsets into SSTable file footers during `SstWriter` disk flushes, allowing `SstReader` to evaluate candidate key existence in memory before triggering disk page reads.

### 2. Write-Ahead Logging (WAL) & Crash Recovery
Implement an append-only Write-Ahead Log (WAL) to provide zero-data-loss durability for active `MemTable` writes across process restarts.

### 3. Leveled Compaction Architecture
Evolve the `CompactionManager` into a multi-level LSM compaction pipeline (L0, L1... Ln) to bound write amplification and optimize space amplification.

### 4. Vector API Acceleration for Bloom Filters
Leverage Java's **Vector API** (`jdk.incubator.vector`) to vectorize Murmur3 hash calculations across SIMD vector registers, accelerating Bloom filter hash evaluations.

### 5. Block Cache & Range Scans
Introduce an off-heap LRU Block Cache for SSTable data blocks and extend the `TangoDB` API to support concurrent range scans (`iterator(startKey, endKey)`).
