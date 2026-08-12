package io.tango.benchmark.endToEnd;


import io.tango.api.TangoConfig;
import io.tango.api.TangoDB;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(
        iterations = 5,
        time = 2,
        timeUnit = TimeUnit.SECONDS
)
@Measurement(
        iterations = 5,
        time = 3,
        timeUnit = TimeUnit.SECONDS
)
@Fork(2)
public class TangoPutBenchmark {

    private static final int KEY_COUNT = 1_000_000;
    private static final int VALUE_SIZE = 100;

    private TangoDB db;

    private byte[][] keys;

    private byte[] value;

    private int index;

    @Setup(Level.Trial)
    public void setup() {

        TangoConfig config = TangoConfig.builder().build();

        // Configure a dedicated benchmark directory.
        // Example:
        // config.setDataDirectory("benchmark-data");

        db = TangoDB.open(config);

        keys = new byte[KEY_COUNT][];

        for (int i = 0; i < KEY_COUNT; i++) {
            keys[i] = (
                    "benchmark-key-" + i
            ).getBytes(StandardCharsets.UTF_8);
        }

        value = new byte[VALUE_SIZE];

        index = 0;
    }

    @Benchmark
    public void put() {

        db.put(
                keys[index],
                value
        );

        index++;

        if (index == KEY_COUNT) {
            index = 0;
        }
    }

    @TearDown(Level.Trial)
    public void teardown() {

        if (db != null) {
            db.close();
        }
    }
}

