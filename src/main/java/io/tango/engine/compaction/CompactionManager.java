package io.tango.engine.compaction;


import io.tango.api.TangoConfig;
import io.tango.common.util.CompactionCleanupHelper;
import io.tango.engine.memtable.ArenaAllocator;
import io.tango.engine.sstable.SstManager;
import io.tango.model.*;

import java.util.List;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CompactionManager {

    private final StorageMetricsData metricsData;
    private final ExecutorService executorService;
    private final CompactionWorker worker;
    private final TangoConfig config;
    private volatile boolean running;


    private final Semaphore compactionSignal = new Semaphore(0);
    private static final int CORE_THREAD = 1;
    private static final int MAX_THREAD = 1;
    private SstManager sstManager;

    private static final Logger log =
            LoggerFactory.getLogger(CompactionManager.class);

    public CompactionManager(StorageMetricsData metricsData, TangoConfig config, SstManager sstManager) {
        this.metricsData = metricsData;
        this.sstManager = sstManager;
        this.config = config;
        this.executorService =
                new ThreadPoolExecutor(
                        CORE_THREAD,
                        MAX_THREAD,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<>(),
                        Thread.ofPlatform()
                                .name("tango-compaction-worker")
                                .factory()
                );
        this.worker = new CompactionWorker();
    }

    void runWorker() {


        while (!Thread.currentThread().isInterrupted()) {

            try {

                compactionSignal.acquire();


                while (running && metricsData.getL0SstCount() > 2) {
                    CompactionCandidate candidate = selectCompactionCandidate();

                    if (null == candidate) {
                        break;
                    }
                    log.info("Initiating compaction process");
                    CompactionResult result = worker.runCompaction(this.sstManager.nextTableId(), candidate.leftTableId(), candidate.rightTableId(), config.getSstableDirectory());
                    sstManager.addSst(result.newTableId());
                    sstManager.removeSsts(candidate.leftTableId(), candidate.rightTableId());
                    CompactionCleanupHelper.cleanup(candidate.leftTableId(), candidate.rightTableId(), config.getSstableDirectory());
                    log.info("Compaction process completed");


                }


            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
                break;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void init() {
        running = true;
        executorService.submit(this::runWorker);
    }


    void shutdown() {
        if (!running) {
            return;
        }

        running = false;

        compactionSignal.release();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(
                        5,
                        TimeUnit.SECONDS)) {

                    System.err.println(
                            "Compaction worker did not terminate cleanly."
                    );
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();

        }
    }

    public void signalCompaction() {
        compactionSignal.release();
    }

    public CompactionCandidate selectCompactionCandidate() {

        List<SstWriteMetadata> snapshot =
                sstManager.getSstSnapshot();

        if (sstManager.getSstSnapshot().size() < 2) {
            return null;
        }

        SstWriteMetadata left = snapshot.get(0);
        SstWriteMetadata right = snapshot.get(1);

        return new CompactionCandidate(
                left.getTableId(),
                right.getTableId()
        );
    }

}
