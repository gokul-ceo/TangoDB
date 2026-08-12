---
sidebar_position: 4
title: Concurrency & Design Decisions
---

# Concurrency Model & Design Decisions

This document outlines the **concurrency model** of TangoDB and summarizes the core **design decisions and tradeoffs** made during V1 development.

---

## Concurrency Model

TangoDB is designed for highly concurrent multi-threaded workloads where multiple application threads concurrently invoke `put()`, `get()`, and `delete()` operations while background threads handle disk flushing and compaction.

```text
 Client Thread 1 ──┐
 Client Thread 2 ──┼──► [ Active MemTable ] ──► Atomic Freeze ──► [ Async Flush Queue ]
 Client Thread N ──┘            │                                        │
                                │                                        ▼
 Client Reader Threads ─────────┴──────────────────────────────► [ SstManager / Disk ]
```

### Key Concurrency Mechanics

1. **Lock-Free Active Ingest**:
   Active `MemTable` insertions use Java's lock-free `ConcurrentSkipListMap` for index updates paired with atomic native memory allocation offset increments.

2. **Atomic MemTable Freeze & Swap**:
   When the active `MemTable` reaches its memory limit (`memTableSize`), an atomic reference swap replaces the active table with a fresh `MemTable`. Write threads experience zero blocking locks.

3. **Immutable Reader Isolation**:
   Frozen `ImmutableMemTable`s and written SSTables are strictly read-only. Reader threads can safely search active tables, frozen tables, and SSTables concurrently without acquiring read-locks.

4. **Background Flush & Compaction Isolation**:
   Disk I/O is offloaded to dedicated background thread pools (`FlushManager` and `CompactionManager`), ensuring client write latencies are completely decoupled from disk speed.

---

## Summary of Key Design Decisions

### 1. Off-Heap FFM API vs. On-Heap Java Objects
* **Decision**: Store record byte payloads off-heap using JDK 22+ FFM API (`MemorySegment` & `Arena`) while maintaining a lightweight on-heap index (`ConcurrentSkipListMap`).
* **Why**: Eliminates JVM Garbage Collection pauses during heavy write traffic.

### 2. Fixed-Size 64 MiB Arenas vs. Dynamic Sizing
* **Decision**: Allocate native memory blocks in fixed 64 MiB `Arena` units per `MemTable`.
* **Why**: Simplifies native memory fragmentation management and enables deterministic block deallocation upon flushing.

### 3. Memory-Mapped File I/O vs. Traditional Byte Streams
* **Decision**: Use `FileChannel.map` wrapped in FFM `MemorySegment` for SSTable reading (`SstReader`).
* **Why**: Offloads OS page caching to the kernel and enables zero-copy native memory slicing during record lookups.

### 4. Lock-Free Atomic Table Swapping vs. Coarse Synchronization
* **Decision**: Use lock-free state transitions when freezing full `MemTable`s.
* **Why**: Prevents write thread stalls during high-concurrency write surges.
