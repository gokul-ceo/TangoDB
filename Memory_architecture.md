## TangoDB Memory Architecture & Memory Format

TangoDB separates the **record data** from the **in-memory index**.

The actual record payload is stored **off-heap** using Java's Foreign Function & Memory API. The in-memory index is maintained on the Java heap and stores a mapping between a key and the memory offset of its corresponding record.

```text
            ConcurrentSkipListMap
                    │
                    │ key → offset
                    ▼
          ┌───────────────────┐
          │   Arena / Memory  │
          │                   │
          │    Record 1       │
          │    Record 2       │
          │    Record 3       │
          │       ...         │
          └───────────────────┘
```

### Record Storage

A Record is stored in a `MemoreySegment`, while the index stores the offset required to locate that record

#### Conceptually, a `put` follows this path:
```
PUT(key, value)
│
├── Allocate record in Arena
│
├── Write record to MemorySegment
│
└── index.put(key, offset)
```

### Arena

An `Arena` provides the memory region used by a MemTable to store its record data.

Each MemTable is backed by an Arena with a default capacity of **64 MiB**.

```text
MemTable
    │
    └── Arena (64 MiB)
          │
          ├── Record 1
          ├── Record 2
          ├── Record 3
          ├── ...
          └── Record N
          
```

As records are inserted, the Arena's available memory is consumed.

Once the Arena reaches its configured capacity

```
Arena Full
    │
    ▼
Freeze MemTable
    │
    ▼
Create ImmutableMemTable
    │
    ▼
Submit for Flush
    │
    ▼
Create SSTable
```

### Record Format

A record is a logical block that holds important information about the data



```
┌──────────────────────────────┐
│ Flag                         │
├──────────────────────────────┤
│ Key Length                   │
├──────────────────────────────┤
│ Value Length                 │
├──────────────────────────────┤
│ Key Bytes                    │
├──────────────────────────────┤
│ Value Bytes                  │
└──────────────────────────────┘
```

This approach give TangoDB explicit control over
1. Record layout
2. Field size
3. Memory offsets
4. Alignment
5. Memory lifetime
6. Off-heap allocation

