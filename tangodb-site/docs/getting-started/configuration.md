---
sidebar_position: 2
title: Configuration
---

# Configuration Options

TangoDB uses an immutable builder pattern through `io.tango.api.TangoConfig` to customize engine behavior, memory sizing, disk directories, and background compaction tasks.

---

## Configuration Properties

| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `sstableDirectory` | `Path` | `Path.of("data")` | Directory path where written SSTable data files and index metadata are persisted. |
| `memTableSize` | `long` | `67,108,864` (64 MiB) | Maximum capacity in bytes of an active `MemTable`. Once reached, the table freezes and triggers asynchronous disk flush. |
| `arenaBlockSize` | `long` | `65,536` (64 KiB) | Memory block allocation size used by `ArenaAllocator` for off-heap native memory segment allocation. |
| `flushQueueSize` | `int` | `8` | Bounded size of the asynchronous flush queue for background SSTable flushing tasks. |
| `enableCompaction` | `boolean` | `true` | Enables or disables background SSTable merging and tombstone garbage collection. |

---

## Example Builder Usages

### Standard Production Setup

```java
import io.tango.api.TangoConfig;
import java.nio.file.Path;

TangoConfig config = TangoConfig.builder()
        .sstableDirectory(Path.of("/var/lib/tangodb/data"))
        .memTableSize(128L * 1024 * 1024) // 128 MiB MemTable
        .arenaBlockSize(128L * 1024)       // 128 KiB Block Size
        .flushQueueSize(16)
        .build();
```

### Fast In-Memory Testing Setup

For small-scale benchmarks or unit tests, setting a lower `memTableSize` triggers frequent background flushes:

```java
TangoConfig testConfig = TangoConfig.builder()
        .sstableDirectory(Path.of("target/test-data"))
        .memTableSize(64L * 1024) // 64 KiB MemTable for quick flushes
        .arenaBlockSize(4L * 1024)
        .disableCompaction()      // Disable background compaction during specific tests
        .build();
```

---

## Validation & Safety Rules

When `TangoConfig.builder().build()` is invoked, the following invariant checks are strictly enforced:

1. **`memTableSize` must be positive**: `memTableSize > 0`
2. **`arenaBlockSize` must be positive**: `arenaBlockSize > 0`
3. **`flushQueueSize` must be positive**: `flushQueueSize > 0`
4. **`memTableSize` >= `arenaBlockSize`**: The total MemTable capacity cannot be smaller than an individual native allocation block.
