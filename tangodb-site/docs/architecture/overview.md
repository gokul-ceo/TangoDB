---
sidebar_position: 1
title: Storage Engine Design
---

# Storage Engine Design

TangoDB implements a **Log-Structured Merge-tree (LSM-tree)** storage engine optimized for high-throughput concurrent writes, predictable memory overhead, and minimal read latencies on modern Java runtimes.

---

## LSM-Tree Pipeline Architecture

The storage engine relies on a multi-tiered pipeline that converts random client write requests into sequential disk I/O operations.

```text
┌─────────────────────────────────────────────────────────────┐
│                       Client Request                        │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
       PUT / DELETE(Key, Val)              GET(Key)
               │                               │
               ▼                               ▼
     ┌───────────────────┐           ┌───────────────────┐
     │  Active MemTable  │           │ 1. Active MemTable│
     └─────────┬─────────┘           └─────────┬─────────┘
               │ MemTable Full                 │ Not found
               ▼                               ▼
     ┌───────────────────┐           ┌───────────────────┐
     │ ImmutableMemTable │           │ 2. Immutable Tables│
     └─────────┬─────────┘           └─────────┬─────────┘
               │ Async Queue                   │ Not found
               ▼                               ▼
     ┌───────────────────┐           ┌───────────────────┐
     │   Flush Manager   │           │ 3. SSTable Readers│
     └─────────┬─────────┘           │ (Mapped ByteBuf)  │
               │ Write .sst            └───────────────────┘
               ▼
     ┌───────────────────┐
     │ Compaction Worker │
     └───────────────────┘
```

---

## Operation Execution Paths

### 1. Write Execution Path (`put`)
When a client thread invokes `put(key, value)`:
1. **Payload Formatting**: The engine formats the record flag (`PUT`), key length, value length, key bytes, and value bytes into a contiguous binary representation.
2. **Off-Heap Allocation**: The binary block is written directly into native memory managed by the active `MemTable`'s FFM `Arena`.
3. **Index Mapping**: A lock-free insertion is made into the `MemTable`'s `ConcurrentSkipListMap` index, mapping the key byte wrapper to the native memory offset.
4. **Capacity Threshold Check**: If the active `MemTable`'s allocated memory exceeds `memTableSize`, the active `MemTable` is atomically frozen into an `ImmutableMemTable`, a fresh active `MemTable` is assigned, and the frozen table is enqueued for background disk flushing.

### 2. Read Execution Path (`get`)
When a client thread invokes `get(key)`:
1. **Active MemTable Lookup**: The engine queries the active `MemTable` index. If the key exists, the value byte array is read directly from native memory and returned.
2. **Immutable MemTables Lookup**: If not found, the engine inspects pending `ImmutableMemTable`s in reverse chronological order (newest to oldest).
3. **SSTable Disk Lookup**: If still not found, `SstReader` evaluates persisted SSTables in reverse chronological order:
   - Memory-maps the `.sst` file via `FileChannel.map(FileChannel.MapMode.READ_ONLY, 0, length)` wrapped in a `MemorySegment`.
   - Validates header metadata (Magic number `0x47534442` and Version `1`).
   - Scans records to match key bytes and returns value payload or tombstone status (`DELETED`).

### 3. Delete Execution Path (`delete`)
Deletions in TangoDB follow LSM-tree **tombstone semantics**:
1. Instead of physically removing records from native memory or disk immediately, `delete(key)` writes a special record with a `DELETE` flag (tombstone).
2. The tombstone overrides earlier versions of the key during read lookups.
3. During background **Compaction**, tombstones are garbage-collected and permanently purged alongside older obsolete key versions.

---

## Why LSM-Tree for Modern Java?

Traditional in-place update storage engines (such as B-Trees) require random page updates on disk and frequent lock synchronization across threads.

By contrast, the LSM-tree model:
- **Transforms Random Writes into Sequential Allocations**: Writes hit off-heap native memory sequentially and stream to disk as immutable contiguous files.
- **Reduces GC Pressure**: Off-heap native memory management avoids JVM heap allocation spikes for raw record payloads during high-throughput ingest.
- **Simplifies Thread Safety**: `ImmutableMemTable`s and SSTables are strictly immutable once frozen, eliminating reader-writer lock contention.
