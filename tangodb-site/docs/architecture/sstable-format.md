---
sidebar_position: 3
title: SSTable Format
---

# SSTable Format & Persistence

When an `ImmutableMemTable` is flushed to persistent storage, it is serialized into an **SSTable (Sorted String Table)** file.

---

## SSTable File Binary Layout

In TangoDB V1, an SSTable is an immutable binary data file structured with a fixed header followed by contiguous sorted key-value records:

```text
┌──────────────────────────────────────────────────────────────┐
│                    1. Header Block                           │
│  Magic (0x47534442) | Version (1) | Record Count | Data Offset│
├──────────────────────────────────────────────────────────────┤
│                    2. Data Block (Sorted Records)            │
│  Record 1 [Flag | KeyLen | ValLen | KeyBytes | ValBytes]     │
│  Record 2 [Flag | KeyLen | ValLen | KeyBytes | ValBytes]     │
│  ...                                                         │
│  Record N [Flag | KeyLen | ValLen | KeyBytes | ValBytes]     │
└──────────────────────────────────────────────────────────────┘
```

### Header Fields

1. **Magic Number (`0x47534442`)**: 4-byte integer magic identifier verifying file integrity.
2. **Version (`1`)**: 4-byte integer version layout identifier.
3. **Record Count**: Total number of key-value records stored in the file.
4. **Data Offset**: Starting byte position of the contiguous record data block.

---

## SSTable Lifecycle in V1

1. **Sequential Flush (`SstWriter`)**: `FlushManager` streams records from native off-heap memory to disk in sorted order.
2. **Memory-Mapped Read (`SstReader`)**: `SstReader` opens the SSTable via memory-mapped buffers (`FileChannel.map`), extracts page metadata header via `SstHeaderExtractor`, and scans slices for requested keys.
3. **Merge Compaction**: `CompactionManager` reads multiple SSTables via `SstIterator`, performs a multi-way merge-sort, eliminates tombstones, and outputs consolidated SSTable files.
