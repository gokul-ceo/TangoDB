package io.tango.engine.flush;



import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.engine.sstable.SstManager;
import io.tango.exception.TangoDBException;
import io.tango.model.StorageMetricsData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlushManager {

    private final SstManager sstManager;
    private final Path ssTablePath;
    private final StorageMetricsData metricsData;
    private Thread worker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "tango-flush-worker");
        t.setDaemon(false);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(true);
    private static final Logger log =
            LoggerFactory.getLogger(FlushManager.class);

    public FlushManager(Path writePath, SstManager sstManager, StorageMetricsData metricsData) {
        this.sstManager = sstManager;
        this.ssTablePath = writePath;
        this.metricsData = metricsData;
    }

    private final BlockingQueue<FlushTask> queue = new LinkedBlockingQueue<>(8);

    public void submit(FlushTask task) {

        if (!running.get()) {
            throw new IllegalStateException("FlushManager is shutting down.");
        }

        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TangoDBException("Interrupted while waiting for flush queue", e);
        }

    }


    public Iterator<ImmutableMemTable> descendingIterator() {

        return queue.stream()
                .filter(FlushTable.class::isInstance)
                .map(FlushTable.class::cast)
                .map(FlushTable::table)
                .iterator();

    }

    public void start() {
        executor.submit(this::runWorker);
    }

    public void runWorker() {
        while (true) {
            try {
                FlushTask task = queue.take();

                if (task == ShutdownTask.INSTANCE) {
                    break;
                }
                FlushTable flush =
                        (FlushTable) task;

                ImmutableMemTable table = flush.table();
                log.info("Flush started");
                sstManager.write(table, ssTablePath);
                log.info("Flush completed");
                table.close();
                log.info("Arena space has been released");


            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log the error and continue processing future flushes
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {

        running.set(false);

        executor.shutdown();

        try {

            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

        } catch (InterruptedException e) {

            executor.shutdownNow();

            Thread.currentThread().interrupt();
        }
    }

}
