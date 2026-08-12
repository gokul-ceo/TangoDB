# Contributing to TangoDB

Thank you for your interest in contributing to **TangoDB**! 

TangoDB is an experimental high-performance embedded key-value storage engine built from scratch in modern Java. We welcome contributions of all kinds — whether you are fixing bugs, improving documentation, adding new features, optimizing storage algorithms, or running performance benchmarks.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Prerequisites & Environment Setup](#prerequisites--environment-setup)
3. [Building & Testing](#building--testing)
4. [Running Benchmarks](#running-benchmarks)
5. [Architecture & Core Concepts](#architecture--core-concepts)
6. [Coding Guidelines & Best Practices](#coding-guidelines--best-practices)
7. [Submitting a Pull Request](#submitting-a-pull-request)
8. [Reporting Issues](#reporting-issues)

---

## Code of Conduct

All contributors and maintainers are expected to abide by our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before participating in community discussions or submitting code.

---

## Prerequisites & Environment Setup

To work on TangoDB, ensure you have the following installed:

* **Java JDK 24+**: TangoDB uses modern Java 24 features including the **Foreign Function & Memory (FFM) API** (`java.lang.foreign.*`).
* **Apache Maven 3.9+**: Build tool.
* **IDE**: IntelliJ IDEA (recommended), Eclipse, or VS Code with JDK 24 enabled.

### Cloning the Repository

```bash
git clone https://github.com/gokul-ceo/TangoDB.git
cd TangoDB
```

---

## Building & Testing

### Compile the Project

```bash
mvn clean compile
```

### Run Unit & Integration Tests

We use JUnit 5 for testing. Run all unit tests with:

```bash
mvn test
```

To run a specific test class:

```bash
mvn test -Dtest=TangoDBTest
```

---

## Running Benchmarks

TangoDB relies heavily on performance analysis using **JMH (Java Microbenchmark Harness)**.

### Run JMH Benchmarks

```bash
mvn test-compile exec:exec
```

> **Note for Performance Contributions:**
> If your Pull Request alters core execution paths (e.g., MemTable insertions, SSTable searches, I/O serialization, or compaction), please include before-and-after JMH benchmark results in your PR description.

---

## Architecture & Core Concepts

Before submitting major structural changes, familiarize yourself with TangoDB's key subsystems:

* **MemTable & SkipList**: In-memory concurrent buffer backed by modern off-heap memory primitives.
* **Foreign Function & Memory (FFM) API**: Off-heap allocation using `MemorySegment` and `Arena` to eliminate GC overhead during throughput peaks.
* **SSTables (Sorted String Tables)**: Immutable disk-backed files structured for range scans and fast point lookups.
* **Asynchronous Flushing & Compaction**: Background threads that drain full MemTables to disk and merge SSTables to maintain read performance.

Refer to [Architecture.md](Architecture.md) and [Memory_architecture.md](Memory_architecture.md) for deeper architectural details.

---

## Coding Guidelines & Best Practices

1. **Modern Java Primitives**:
   * Utilize `MemorySegment`, `Arena`, and `VarHandle` for off-heap interactions.
   * Ensure `Arena` lifetimes are managed strictly and closed deterministically without memory leaks.
2. **Thread Safety & Concurrency**:
   * Storage engine components are heavily concurrent. Ensure thread safety without resorting to global locks where non-blocking or fine-grained locking techniques can be applied.
3. **No Unsafe / Raw Pointer Hacks**:
   * Stick to Java's official Foreign Function & Memory API (`java.lang.foreign.*`).
4. **Code Formatting & Cleanliness**:
   * Follow standard Java naming conventions (PascalCase for classes, camelCase for methods/variables, UPPER_SNAKE_CASE for constants).
   * Avoid raw print statements (`System.out.println`) in core storage code — use `slf4j` logging instead.
5. **Test Coverage**:
   * New features and bug fixes MUST include corresponding unit tests under `src/test/java/`.

---

## Submitting a Pull Request

1. **Fork the Repository**: Create your personal fork on GitHub.
2. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/memtable-optimization
   # or
   git checkout -b fix/compaction-deadlock
   ```
3. **Make & Test Your Changes**: Ensure `mvn clean test` passes cleanly.
4. **Commit Your Changes**: Write clear, descriptive commit messages:
   ```bash
   git commit -m "feat(memtable): optimize lock-free skip list insertion"
   ```
5. **Push & Open a PR**: Push your branch to GitHub and submit a Pull Request targeting the `main` branch.
6. **Fill out the PR Template**: Detail the changes made, tests added, and performance impact.

---

## Reporting Issues

* **Bug Reports**: Open an issue describing the environment (OS, Java version), steps to reproduce, expected vs actual behavior, and relevant logs.
* **Feature Requests**: Open an issue proposing the feature, the use case, and potential implementation ideas.

Thank you for helping make TangoDB faster, more reliable, and awesome! 🚀
