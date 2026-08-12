
# TangoDB Architecture

 TangoDB follows an LSM-tree-inspired storage architecture.

                    ┌──────────────┐
                    │   TangoDB    │
                    │     API      │
                    └──────┬───────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │    MemTable     │
                  │                 │
                  │ Concurrent      │
                  │ SkipList        │
                  │       +         │
                  │ MemorySegment   │
                  └────────┬────────┘
                           │
                     MemTable Full
                           │
                           ▼
                 ┌─────────────────────┐
                 │  Immutable MemTable │
                 └──────────┬──────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Flush Manager │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    SSTable    │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   Compaction  │
                    └───────────────┘