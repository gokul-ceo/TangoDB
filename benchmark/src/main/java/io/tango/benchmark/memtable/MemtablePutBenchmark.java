package io.tango.benchmark.memtable;

import io.tango.api.TangoConfig;
import io.tango.common.constants.BlockFlag;
import io.tango.engine.memtable.MemTable;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 10, time = 3)
@Fork(2)
@State(Scope.Thread)
public class MemtablePutBenchmark {

    private MemTable memTable;

    private byte[][] keys;
    private byte[] value;

    private int keyIndex;

    @Setup(Level.Iteration)
    public void setupIteration() {

        memTable = new MemTable(TangoConfig.builder().build());

        keyIndex = 0;
    }

    @TearDown(Level.Iteration)
    public void teardownIteration() {

        memTable.close();
    }

    @Setup(Level.Trial)
    public void setup() {

        TangoConfig config = TangoConfig.builder().build();

        memTable = new MemTable(config);

        int keyCount = 1_000_000;

        keys = new byte[keyCount][];

        for (int i = 0; i < keyCount; i++) {
            keys[i] = ("key-" + i)
                    .getBytes(StandardCharsets.UTF_8);
        }

        value = new byte[100];
    }

    @Setup(Level.Iteration)
    public void reset() {
        keyIndex = 0;
    }

    @Benchmark
    public void put() {

        byte[] key = keys[keyIndex++ % keys.length];

        memTable.put(
                key,
                value,
                BlockFlag.DEFAULT
        );
    }

    @TearDown(Level.Trial)
    public void teardown() {
        memTable.close();
    }
}