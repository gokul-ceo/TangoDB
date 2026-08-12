package io.tango.engine.sstable;


import io.tango.engine.compaction.CompactionManager;
import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.exception.TangoDBException;
import io.tango.model.SsTableResult;
import io.tango.model.SstWriteMetadata;
import io.tango.model.StorageMetricsData;
import io.tango.api.TangoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class SstManager {

    private static final Logger log = LoggerFactory.getLogger(SstManager.class);

    public static final int HEADER_SIZE = 256;
    public static final int DATA_START_OFFSET = HEADER_SIZE;
    private final StorageMetricsData metricsData;
    private final SstWriter writer;
    private final TangoConfig config;
    private final SstReader reader;
    private final CompactionManager compactionManager;
    private final AtomicLong nextTableId = new AtomicLong(1);
    private final AtomicReference<List<SstWriteMetadata>> sstSnapshot = new AtomicReference<>(List.of());

//    private final List<SstWriteMetadata> mutableSstTables = new ArrayList<>();

    public SstManager(StorageMetricsData metricsData, TangoConfig config) {
        this.metricsData = metricsData;
        this.writer = new SstWriter();
        this.reader = new SstReader();
        this.config = config;
        if (config.isCompactionEnabled()) {
            this.compactionManager =
                    new CompactionManager(metricsData, config, this);

            this.compactionManager.init();
        } else {
            this.compactionManager = null;
        }

    }


    public void write(ImmutableMemTable table, Path directory) {

        long tableId = nextTableId();

        writer.write(tableId, table, directory);

        addSst(tableId);

        metricsData.incrementL0SstCount();

        if (compactionManager != null) {
            compactionManager.signalCompaction();
        }
    }


    public SsTableResult read(byte[] searchKey, Path dataDir) {
        List<SstWriteMetadata> snapshot = sstSnapshot.get();
        return reader.read(searchKey, dataDir, snapshot);
    }


    public void load(Path directory) {

        if (Files.notExists(directory)) {
            log.info("SSTable directory does not exist: {}", directory);
            sstSnapshot.set(List.of());
            metricsData.setL0SstCount(0);
            nextTableId.set(1);
            return;
        }

        List<SstWriteMetadata> loadedTables = new ArrayList<>();

        long maxId = 0;

        log.info("Loading SSTables from {}", directory);

        try (Stream<Path> stream = Files.list(directory)) {

            for (Path path : stream.filter(Files::isRegularFile).filter(path -> path.getFileName().toString().endsWith(".sgdb")).toList()) {

                long id = parseTableId(path);

                loadedTables.add(new SstWriteMetadata(id));

                maxId = Math.max(maxId, id);
            }

        } catch (IOException e) {

            throw new TangoDBException("Unable to load SSTables", e);
        }


        loadedTables.sort((left, right) -> Long.compare(left.getTableId(), right.getTableId()));

        List<SstWriteMetadata> snapshot =
                List.copyOf(loadedTables);

        sstSnapshot.set(snapshot);


        nextTableId.set(maxId + 1);


        metricsData.setL0SstCount(snapshot.size());

        log.info(
                "Loaded count={} SSTables, nextTableId={}",
                snapshot.size(),
                maxId + 1
        );

    }

    public long nextTableId() {
        return nextTableId.getAndIncrement();
    }

    private long parseTableId(Path path) {

        String fileName = path.getFileName().toString();

        int dot = fileName.indexOf('.');

        if (dot <= 0) {
            throw new TangoDBException("Invalid SSTable filename: " + fileName);
        }

        try {

            return Long.parseLong(fileName.substring(0, dot));

        } catch (NumberFormatException e) {

            throw new TangoDBException("Invalid SSTable ID: " + fileName, e);
        }
    }

    public List<SstWriteMetadata> getSstSnapshot() {
        return sstSnapshot.get();
    }

    public synchronized void addSst(long tableId) {

        sstSnapshot.updateAndGet(old -> {
            List<SstWriteMetadata> updated =
                    new ArrayList<>(old);
            updated.add(new SstWriteMetadata(tableId));
            return List.copyOf(updated);
        });


    }


    public synchronized void removeSsts(long leftTableId, long rightTableId) {

        sstSnapshot.updateAndGet(old -> {
            List<SstWriteMetadata> updated = new ArrayList<>(old);

            updated.removeIf(metadata ->
                    metadata.getTableId() == leftTableId ||
                            metadata.getTableId() == rightTableId
            );

            return List.copyOf(updated);
        });

    }


}
