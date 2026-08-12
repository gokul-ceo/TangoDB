---
sidebar_position: 1
title: Java API Reference
---

# Java API Reference

Detailed reference guide for TangoDB public interfaces, classes, builders, and exception types.

---

## `io.tango.api.TangoDB`

The primary interface for interacting with a TangoDB instance. `TangoDB` implements `AutoCloseable`.

```java
public interface TangoDB extends AutoCloseable {

    /**
     * Inserts or updates a key-value entry in the storage engine.
     *
     * @param key   non-null byte array representing the key
     * @param value non-null byte array representing the value payload
     */
    void put(byte[] key, byte[] value);

    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key non-null byte array representing the key
     * @return byte array value if key exists; null otherwise
     */
    byte[] get(byte[] key);

    /**
     * Deletes a key from the storage engine by writing a tombstone record.
     *
     * @param key non-null byte array representing the key to delete
     */
    void delete(byte[] key);

    /**
     * Closes the database instance, flushes active MemTables to disk,
     * releases native off-heap Arena memory, and shuts down background executor threads.
     */
    @Override
    void close();

    /**
     * Factory method to construct and open a TangoDB instance with configuration.
     *
     * @param config configured TangoConfig instance
     * @return initialized TangoDB instance
     */
    static TangoDB open(TangoConfig config) {
        return new TangoDBImpl(config);
    }
}
```

---

## `io.tango.api.TangoConfig`

Configuration object built via `TangoConfig.builder()`.

```java
public final class TangoConfig {

    public Path getSstableDirectory();
    public long getMemTableSize();
    public long getArenaBlockSize();
    public int getFlushQueueSize();
    public boolean isCompactionEnabled();

    public static Builder builder();

    public static final class Builder {
        public Builder sstableDirectory(Path directory);
        public Builder memTableSize(long bytes);
        public Builder arenaBlockSize(long bytes);
        public Builder flushQueueSize(int size);
        public Builder disableCompaction();
        public TangoConfig build();
    }
}
```

---

## `io.tango.engine.StorageEngine`

The inner engine coordinator managing `MemTable`, `FlushManager`, `SstManager`, and `CompactionManager`.

```java
public final class StorageEngine implements AutoCloseable {
    public StorageEngine(TangoConfig config);
    public void put(byte[] key, byte[] value);
    public byte[] get(byte[] key);
    public void delete(byte[] key);
    public void close();
}
```

---

## Exception Hierarchy (`io.tango.exception`)

| Exception | Extends | Description |
| :--- | :--- | :--- |
| `TangoException` | `RuntimeException` | Base unchecked exception for all TangoDB storage engine operational failures. |
| `StorageEngineException` | `TangoException` | Thrown when I/O operations, disk flushes, or native arena memory allocations fail. |
| `CorruptedSstException` | `TangoException` | Thrown when reading an invalid or corrupted SSTable file format. |
