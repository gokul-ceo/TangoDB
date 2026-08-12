package io.tango;

import io.tango.engine.StorageEngine;
import io.tango.api.TangoConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    static Path tempDir = Path.of("data");
    static TangoConfig config = TangoConfig.builder().sstableDirectory(tempDir).memTableSize(64 * 1024) // 64KB, small enough to trigger flush easily
            .build();
    static StorageEngine db = new StorageEngine(config);

    public static void main(String[] args) throws InterruptedException {
        {
            int threads = 10;
            java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
            for (int t = 0; t < threads; t++) {
                final int tId = t;
                pool.submit(() -> {
                    for (int i = 0; i < 1000; i++) {
                        db.put(bytes("c_" + tId + "_" + i), bytes("v"));
                    }
                });
            }
            pool.shutdown();
            pool.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);

            for (int t = 0; t < threads; t++) {
                final int tId = t;
                for (int i = 0; i < 1000; i++) {
                  System.out.println("value: "+db.get(bytes("c_" + tId + "_" + i)));
                }
            }
        }

    }

    private static byte[] bytes(String s) {
        if (s == null) return null;
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String string(byte[] b) {
        if (b == null) return null;
        return new String(b, StandardCharsets.UTF_8);
    }
}