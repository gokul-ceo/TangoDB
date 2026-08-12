package io.tango.benchmark.sstable;

import io.tango.api.TangoConfig;
import io.tango.common.constants.BlockFlag;
import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.engine.memtable.MemTable;
import io.tango.engine.sstable.SstManager;
import io.tango.model.StorageMetricsData;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(2)
@State(Scope.Benchmark)
public class SstManagerWriteBenchmark {

    private SstManager sstManager;
    private ImmutableMemTable immutableMemTable;
    private Path directory;

    @Param({"1000", "10000", "100000"})
    private int records;

    @Setup(Level.Trial)
    public void setup() throws Exception {

        directory = Files.createTempDirectory(
                "tango-sst-write-benchmark"
        );

        TangoConfig config = TangoConfig.builder()
                .sstableDirectory(directory)
                .disableCompaction()
                .build();

        StorageMetricsData metrics =
                new StorageMetricsData();

        sstManager = new SstManager(
                metrics,
                config
        );

        /*
         * Prepare the MemTable outside the measured operation.
         *
         * This benchmark measures:
         *
         *     ImmutableMemTable
         *             ↓
         *         SstManager
         *             ↓
         *          SstWriter
         *             ↓
         *            Disk
         *
         * MemTable.put() is intentionally excluded.
         */
        MemTable memTable = new MemTable(config);

        for (int i = 0; i < records; i++) {

            byte[] key =
                    ("benchmark-key-" + i)
                            .getBytes(StandardCharsets.UTF_8);

            byte[] value =
                    ("benchmark-value-" + i)
                            .getBytes(StandardCharsets.UTF_8);

            memTable.put(
                    key,
                    value,
                    BlockFlag.DEFAULT
            );
        }

        immutableMemTable = memTable.freeze();
    }

    @Benchmark
    public void write() {

        sstManager.write(
                immutableMemTable,
                directory
        );
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {

        if (immutableMemTable != null) {
            immutableMemTable.close();
        }

        deleteDirectory(directory);
    }

    private void deleteDirectory(Path directory)
            throws Exception {

        if (directory == null || Files.notExists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {

            stream
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}