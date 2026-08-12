---
sidebar_position: 4
title: SSTables & Compaction
---

# SSTables & Compaction

TangoDB persists immutable key-value data to disk in structured **SSTable (Sorted String Table)** data files.

---

## SSTable File Structure

An SSTable in TangoDB consists of two primary components:

```text
┌──────────────────────────────────────────────────────────────┐
│                    Header Block                              │
│  Magic (0x47534442) | Version (1) | Record Count             │
├──────────────────────────────────────────────────────────────┤
│                    Data Block (Sorted Records)               │
│  Record 1 | Record 2 | Record 3 | ... | Record N             │
└──────────────────────────────────────────────────────────────┘
```

---

## Asynchronous Flush Architecture (`FlushManager`)

Flushing occurs asynchronously without stalling incoming active write requests:

```text
Active MemTable Full
        │
        ▼
Freeze -> ImmutableMemTable
        │
        ▼
Submit to FlushManager Queue
        │
        ├── Worker thread picks task
        ├── Opens SstStreamWriter
        ├── Iterates off-heap MemorySegments in sorted key order
        └── Writes .sst file to disk & closes backing native Arena
```

---

## Compaction Engine (`CompactionManager`)

As new SSTable files accumulate, key duplicates and soft-deleted tombstones increase disk storage usage.

The **`CompactionManager`** runs a background worker loop:

1. **SSTable Selection**: Identifies eligible SSTables for merging.
2. **Multi-Way Merge Iterator (`SstIterator`)**: Reads records across SSTables in sorted key sequence.
3. **Deduplication & Tombstone GC**: Keeps only the newest record version for duplicate keys and drops expired tombstones.
4. **New SSTable Generation**: Writes consolidated records into new SSTable files and atomically updates `SstManager` table lists before deleting old SSTable files.
