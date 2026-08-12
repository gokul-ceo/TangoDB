---
sidebar_position: 2
title: Memory Model & FFM Design
---

# Memory Model & FFM Design

A core architectural pillar of TangoDB is its **off-heap native memory architecture** powered by Java's **Foreign Function & Memory (FFM) API** (`java.lang.foreign`).

---

## Architectural Rationale: Why FFM API?

Building high-throughput storage engines on the JVM traditionally suffers from Garbage Collection (GC) latency spikes when managing millions of key-value objects on the Java heap.

TangoDB addresses this challenge by decoupling **record data storage** from **heap object management**:

```text
                  Java Heap Index                      Native Off-Heap Arena
          ┌─────────────────────────────┐        ┌─────────────────────────────┐
          │    ConcurrentSkipListMap    │        │  MemorySegment (FFM API)    │
          │                             │        │                             │
          │  Key "user:1" ──► Offset 0  ├───────►│  [Flag|Len|Val...]          │
          │  Key "user:2" ──► Offset 48 ├───────►│  [Flag|Len|Val...]          │
          │  Key "user:3" ──► Offset 96 ├───────►│  [Flag|Len|Val...]          │
          └─────────────────────────────┘        └─────────────────────────────┘
```

### Design Decision Comparison

| Memory Approach | GC Impact & Pressure | Safety & Portability | Deallocation Control |
| :--- | :--- | :--- | :--- |
| **Java On-Heap Objects** | High GC pressure & pause times under heavy write load | Safe & Portable | Managed by GC non-deterministically |
| **`sun.misc.Unsafe`** | Low GC pressure | Unsafe, deprecated, vendor-locked | Manual pointer arithmetic, error-prone |
| **`ByteBuffer.allocateDirect()`**| Reduced GC pressure | Safe, portable | Buffer slicing limits, non-deterministic cleanup |
| **Java FFM API (TangoDB Choice)** | **Significantly Reduced GC Pressure** | **Safe, standard Java 22+ API** | **Deterministic via `Arena.close()`** |

---

## FFM Abstractions Used

1. **`Arena`**: Controls the allocation lifespan of native memory regions. TangoDB uses scoped `Arena`s per `MemTable` (default 64 MiB capacity).
2. **`MemorySegment`**: Provides contiguous off-heap native memory bounds with strongly typed, aligned reading and writing methods.
3. **Deterministic Memory Lifespan**: When a `MemTable` is frozen and successfully flushed to an SSTable on disk, its backing `Arena` is explicitly closed (`arena.close()`), immediately releasing native memory back to the operating system without waiting for GC passes.

---

## Record Binary Layout Format

Records are serialized off-heap into a compact, fixed-header binary layout:

```text
┌──────────────────────────────┐
│ Flag (1 byte)                │  --> 0x01 = PUT, 0x02 = DELETE (Tombstone)
├──────────────────────────────┤
│ Key Length (4 bytes int)     │  --> Big-endian int length of key
├──────────────────────────────┤
│ Value Length (4 bytes int)   │  --> Big-endian int length of value
├──────────────────────────────┤
│ Key Bytes (N bytes)          │  --> UTF-8 or raw byte sequence of Key
├──────────────────────────────┤
│ Value Bytes (M bytes)        │  --> Raw byte payload of Value
└──────────────────────────────┘
```

### Binary Format Characteristics

* **Fixed 9-Byte Record Header**: 1 byte flag + 4 bytes key length + 4 bytes value length allow deterministic offset computation.
* **Sequential Contiguous Writes**: Incoming write requests allocate contiguous native slices inside the active `MemorySegment`.
* **Zero-Copy Streaming to Disk**: During SSTable flushes, native memory segments can be written directly to file channels without intermediate byte array cloning on the Java heap.
