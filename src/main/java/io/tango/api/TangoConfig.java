package io.tango.api;

import java.nio.file.Path;

/**
 * Immutable configuration class holding runtime settings for a {@link TangoDB} storage engine instance.
 * <p>
 * Instances of {@code TangoConfig} are created using the fluent {@link Builder} accessed via {@link #builder()}.
 * </p>
 *
 * @author Gokul G
 * @version 0.1.0
 */
public final class TangoConfig {

    private final Path sstableDirectory;
    private final long memTableSize;
    private final long arenaBlockSize;
    private final int flushQueueSize;
    private final boolean enableCompaction;

    private TangoConfig(Builder builder) {
        this.sstableDirectory = builder.sstableDirectory;
        this.memTableSize = builder.memTableSize;
        this.arenaBlockSize = builder.arenaBlockSize;
        this.flushQueueSize = builder.flushQueueSize;
        this.enableCompaction = builder.enableCompaction;
    }

    /**
     * Gets the directory path where SSTable files and log files are stored.
     *
     * @return directory {@link Path}
     */
    public Path getSstableDirectory() {
        return sstableDirectory;
    }

    /**
     * Gets the maximum capacity in bytes of an active MemTable before triggering a background flush.
     *
     * @return MemTable threshold size in bytes
     */
    public long getMemTableSize() {
        return memTableSize;
    }

    /**
     * Gets the memory allocation block size in bytes for off-heap FFM native Arenas.
     *
     * @return Arena block size in bytes
     */
    public long getArenaBlockSize() {
        return arenaBlockSize;
    }

    /**
     * Gets the maximum queue capacity for immutable MemTable background flush jobs.
     *
     * @return flush queue capacity
     */
    public int getFlushQueueSize() {
        return flushQueueSize;
    }

    /**
     * Indicates whether background SSTable compaction is enabled.
     *
     * @return {@code true} if compaction is enabled; {@code false} otherwise
     */
    public boolean isCompactionEnabled() {
        return enableCompaction;
    }

    /**
     * Creates a new {@link Builder} initialized with default configuration settings.
     *
     * @return new {@link Builder} instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing immutable {@link TangoConfig} instances with custom parameters.
     */
    public static final class Builder {

        // Defaults
        private Path sstableDirectory = Path.of("data");
        private long memTableSize = 64L * 1024 * 1024;   // 64 MiB
        private long arenaBlockSize = 64L * 1024;        // 64 KiB
        private int flushQueueSize = 8;
        private boolean enableCompaction = true;

        private Builder() {
        }

        /**
         * Disables automatic background SSTable merge compaction.
         *
         * @return this {@link Builder} instance
         */
        public Builder disableCompaction() {
            this.enableCompaction = false;
            return this;
        }

        /**
         * Sets the target disk directory for SSTable storage.
         *
         * @param directory non-null {@link Path} to the storage directory
         * @return this {@link Builder} instance
         */
        public Builder sstableDirectory(Path directory) {
            this.sstableDirectory = directory;
            return this;
        }

        /**
         * Sets the maximum in-memory MemTable size in bytes before initiating asynchronous disk flushing.
         *
         * @param bytes positive capacity in bytes (default: 64 MiB)
         * @return this {@link Builder} instance
         */
        public Builder memTableSize(long bytes) {
            this.memTableSize = bytes;
            return this;
        }

        /**
         * Sets the block size in bytes for native off-heap Arena memory segment allocations.
         *
         * @param bytes positive block size in bytes (default: 64 KiB)
         * @return this {@link Builder} instance
         */
        public Builder arenaBlockSize(long bytes) {
            this.arenaBlockSize = bytes;
            return this;
        }

        /**
         * Sets the maximum number of immutable MemTables allowed in the asynchronous flush queue.
         *
         * @param size positive queue capacity (default: 8)
         * @return this {@link Builder} instance
         */
        public Builder flushQueueSize(int size) {
            this.flushQueueSize = size;
            return this;
        }

        /**
         * Builds and validates a new {@link TangoConfig} instance.
         *
         * @return validated {@link TangoConfig} instance
         * @throws IllegalArgumentException if any configuration parameter fails validation
         */
        public TangoConfig build() {

            if (memTableSize <= 0) {
                throw new IllegalArgumentException("MemTable size must be positive.");
            }

            if (arenaBlockSize <= 0) {
                throw new IllegalArgumentException("Arena block size must be positive.");
            }

            if (flushQueueSize <= 0) {
                throw new IllegalArgumentException("Flush queue size must be positive.");
            }

            if (memTableSize < arenaBlockSize) {
                throw new IllegalArgumentException(
                        "MemTable size cannot be smaller than arena block size.");
            }

            return new TangoConfig(this);
        }
    }
}
