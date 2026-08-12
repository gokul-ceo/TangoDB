package io.tango.engine;


import io.tango.api.TangoConfig;
import io.tango.common.constants.BlockFlag;
import io.tango.common.constants.LookUpStatus;
import io.tango.engine.flush.FlushManager;
import io.tango.engine.flush.FlushTable;
import io.tango.engine.flush.ShutdownTask;
import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.engine.memtable.MemTable;
import io.tango.engine.sstable.SstManager;
import io.tango.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.tango.common.constants.DefaultConstants.DEFAULT_MAX_KEY_SIZE;
import static io.tango.common.constants.DefaultConstants.DEFAULT_MAX_VALUE_SIZE;


public final class StorageEngine implements AutoCloseable {

    private static final Logger log =
            LoggerFactory.getLogger(StorageEngine.class);

    private volatile MemTable liveMemTable;
    private final FlushManager flushManager;
    private final SstManager sstManager;
    private final TangoConfig config;
    private volatile boolean closing;
    private final Thread shutdownHook;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final StorageMetricsData metrics;


    public StorageEngine(TangoConfig config) {

        this.metrics = new StorageMetricsData();
        this.liveMemTable = new MemTable(config);
        this.config = config;
        this.sstManager = new SstManager(this.metrics, config);
        this.sstManager.load(config.getSstableDirectory());
        shutdownHook = new Thread(this::close, "swegodb-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        this.flushManager = new FlushManager(this.config.getSstableDirectory(), this.sstManager, this.metrics);
        this.flushManager.start();

    }

    public void put(byte[] key, byte[] value) {

        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }

        if (key.length > DEFAULT_MAX_KEY_SIZE) {
            throw new IllegalArgumentException("Key size exceeds maximum allowed size: " + DEFAULT_MAX_KEY_SIZE);
        }

        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }

        if (value.length > DEFAULT_MAX_VALUE_SIZE) {
            throw new IllegalArgumentException("Value size exceeds maximum allowed size: " + DEFAULT_MAX_VALUE_SIZE);
        }


        if (closing) {
            throw new IllegalStateException("TangoDB is shutting down.");
        }
        WriteResult writeResult = liveMemTable.put(key, value, BlockFlag.DEFAULT);
        metrics.recordWrite(writeResult.bytesWritten());
        if (!writeResult.success()) {
            ImmutableMemTable immutableMemTable = liveMemTable.freeze();
            liveMemTable = new MemTable(config);
            liveMemTable.put(key, value, BlockFlag.DEFAULT);
            flushManager.submit(new FlushTable(immutableMemTable));
        }
    }

    public byte[] get(byte[] key) {

        MemTableResult result = liveMemTable.get(key);

        if (LookUpStatus.FOUND.equals(result.status())) {
            metrics.recordRead(result.totalBytesRead());
            return result.value();
        }

        for (Iterator<ImmutableMemTable> it = flushManager.descendingIterator(); it.hasNext(); ) {

            result = it.next().get(key);
            metrics.recordRead(result.totalBytesRead());

            if (LookUpStatus.FOUND.equals(result.status())) {
                return result.value();
            }
        }


        SsTableResult ssTableResult = sstManager.read(key, config.getSstableDirectory());

        metrics.recordRead(ssTableResult.totalBytesRead());

        if (LookUpStatus.DELETED.equals(ssTableResult.status())) {
            return null;
        } else if (LookUpStatus.NOT_FOUND.equals(ssTableResult.status())) {
            return null;
        } else if (LookUpStatus.FOUND.equals(ssTableResult.status())) {
            return ssTableResult.value();
        }

        return null;
    }

    public void delete(byte[] key) {
        this.liveMemTable.remove(key);
        metrics.recordTombstone();
    }

    public void shutdown() {

        if (closing) {
            return;
        }

        closing = true;

        log.info("Shutting down storage engine. Waiting for pending flushes...");

        ImmutableMemTable immutableMemTable = liveMemTable.freeze();

        if (immutableMemTable.size() > 0) {
            flushManager.submit(new FlushTable(immutableMemTable));
        }


        // shutdown poison
        flushManager.submit(ShutdownTask.INSTANCE);

        flushManager.shutdown();

        log.info("Storage engine shutdown completed.");
    }


    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        shutdown();
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException ignored) {
            // JVM is already shutting down
        }
    }
}
