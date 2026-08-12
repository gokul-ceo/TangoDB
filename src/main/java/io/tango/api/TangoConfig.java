package io.tango.api;

import java.nio.file.Path;

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
        this.enableCompaction  = builder.enableCompaction;

    }

    public Path getSstableDirectory() {
        return sstableDirectory;
    }

    public long getMemTableSize() {
        return memTableSize;
    }

    public long getArenaBlockSize() {
        return arenaBlockSize;
    }

    public int getFlushQueueSize() {
        return flushQueueSize;
    }

    public boolean isCompactionEnabled(){
        return enableCompaction;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        // Defaults
        private Path sstableDirectory = Path.of("data");
        private long memTableSize = 64L * 1024 * 1024;   // 64 MiB
        private long arenaBlockSize = 64L * 1024;        // 64 KiB
        private int flushQueueSize = 8;
        private boolean enableCompaction = true;

        private Builder() {
        }

        public  Builder disableCompaction(){
            this.enableCompaction = false;
            return this;
        }

        public Builder sstableDirectory(Path directory) {
            this.sstableDirectory = directory;
            return this;
        }

        public Builder memTableSize(long bytes) {
            this.memTableSize = bytes;
            return this;
        }

        public Builder arenaBlockSize(long bytes) {
            this.arenaBlockSize = bytes;
            return this;
        }

        public Builder flushQueueSize(int size) {
            this.flushQueueSize = size;
            return this;
        }

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
