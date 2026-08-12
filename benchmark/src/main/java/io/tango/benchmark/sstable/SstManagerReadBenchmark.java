package io.tango.benchmark.sstable;

import io.tango.api.TangoConfig;
import io.tango.common.constants.BlockFlag;
import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.engine.memtable.MemTable;
import io.tango.engine.sstable.SstManager;
import io.tango.model.SsTableResult;
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
public class SstManagerReadBenchmark {

    private SstManager sstManager;

    private Path directory;

    private byte[][] keys;

    private int index;

    @Setup(Level.Trial)
    public void setup() throws Exception {

        TangoConfig config = TangoConfig.builder().build();

        directory = Files.createTempDirectory(
                "tango-sst-read-benchmark"
        );

        StorageMetricsData metrics =
                new StorageMetricsData();

        sstManager = new SstManager(
                metrics,
                config
        );


        MemTable memTable =
                new MemTable(config);

        int recordCount = 100_000;

        keys = new byte[recordCount][];

        for (int i = 0; i < recordCount; i++) {

            byte[] key =
                    ("benchmark-key-" + i)
                            .getBytes(StandardCharsets.UTF_8);

            byte[] value =
                    ("benchmark-value-" + i)
                            .getBytes(StandardCharsets.UTF_8);

            keys[i] = key;

            memTable.put(
                    key,
                    value,
                    BlockFlag.DEFAULT
            );
        }

        ImmutableMemTable immutable =
                memTable.freeze();


        sstManager.write(
                immutable,
                directory
        );

        immutable.close();

        index = 0;
    }

    @Benchmark
    public SsTableResult read() {

        byte[] key =
                keys[index++ % keys.length];

        return sstManager.read(
                key,
                directory
        );
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {

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