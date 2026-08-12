# TangoDB

TangoDB is an **experimental high-performance embedded key-value storage engine built from scratch in modern Java**.

The project explores how modern JVM capabilities can be used to build a storage engine designed for **high throughput, low latency, concurrent workloads, and efficient memory usage**.

TangoDB uses technologies such as:

- Java Foreign Function & Memory API
- Off-heap memory
- `MemorySegment`
- `Arena`
- Concurrent data structures
- LSM-tree-based storage
- Asynchronous flushing
- SSTables
- Background compaction
- Performance-oriented I/O

> **TangoDB is experimental in implementation maturity, not in its performance goals.**

The current implementation is a foundation for exploring high-performance storage-engine design in Java.

---

## Project Status

**Status: Experimental / Active Development**


TangoDB is currently experimental and should not yet be considered production-ready.

However, the primary objective of the project is to build a **high-performance storage engine**, not merely a reference implementation or educational database.

Performance is a first-class design goal.

```text
High Throughput
       +
Low Latency
       +
Concurrent Writes
       +
Efficient Memory Usage
       +
Efficient I/O
       ↓
    TangoDB
```

TangoDB started with a simple question:

> **How far can modern Java be pushed when building a high-performance storage engine?**


v0.1.0 is the first step toward answering that question.

The current implementation establishes the core storage-engine pipeline — from concurrent writes and off-heap memory to immutable MemTables, asynchronous flushing, SSTables, and compaction — while providing a measurable performance baseline.

The goal from here is not to optimize for benchmark numbers blindly, but to understand where the real costs are, measure them, and systematically remove them.

**TangoDB is still experimental. The architecture will evolve, the implementation will change, and the benchmarks will improve.**

This is the beginning of the experiment, not the end of it.

> **Build. Measure. Understand. Optimize. Repeat.**

---

## Community & Open Source

We welcome community contributions! Please check out the following resources to get involved:

* **[Contributing Guidelines](CONTRIBUTING.md)**: Developer environment setup, coding guidelines, testing, and PR submission process.
* **[Code of Conduct](CODE_OF_CONDUCT.md)**: Standards for a welcoming and inclusive community.
* **[Security Policy](SECURITY.md)**: Guidelines for reporting security vulnerabilities.
* **[Support Guide](SUPPORT.md)**: Where to ask questions and find support.

## License

TangoDB is open-source software released under the **[Apache License 2.0](LICENSE)**.

