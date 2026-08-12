import io.tango.api.TangoDB;
import io.tango.engine.StorageEngine;
import io.tango.api.TangoConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static io.tango.common.constants.DefaultConstants.DEFAULT_MAX_KEY_SIZE;
import static io.tango.common.constants.DefaultConstants.DEFAULT_MAX_VALUE_SIZE;
import static org.junit.jupiter.api.Assertions.*;

public class TangoDBTest {

    @TempDir
    Path tempDir;


    private TangoConfig config;
    private TangoDB db;

    @BeforeEach
    void setUp() {
        config = TangoConfig.builder().sstableDirectory(tempDir).memTableSize(64 * 1024) // 64KB, small enough to trigger flush easily
                .build();
        db = TangoDB.open(config);
    }

    @AfterEach
    void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    private byte[] bytes(String s) {
        if (s == null) return null;
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private String string(byte[] b) {
        if (b == null) return null;
        return new String(b, StandardCharsets.UTF_8);
    }

    private void triggerFlush() {
        byte[] pad = new byte[1024]; // 1KB
        for (int i = 0; i < 70; i++) { // 70KB total, memtable size is 64KB
            db.put(bytes("pad_" + System.nanoTime() + "_" + i), pad);
        }
    }


    @Test
    void test01_putGet() {
        db.put(bytes("key1"), bytes("value1"));
        assertEquals("value1", string(db.get(bytes("key1"))));
    }


    @Test
    void test02_multipleKeys() {
        db.put(bytes("key1"), bytes("value1"));
        db.put(bytes("key2"), bytes("value2"));
        assertEquals("value1", string(db.get(bytes("key1"))));
        assertEquals("value2", string(db.get(bytes("key2"))));
    }


    @Test
    void test03_overwrite() {
        db.put(bytes("key1"), bytes("value1"));
        db.put(bytes("key1"), bytes("value2"));
        assertEquals("value2", string(db.get(bytes("key1"))));
    }

    @Test
    void test04_missingKey() {
        assertNull(db.get(bytes("missing")));
    }


    @Test
    void test05_emptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            db.put(new byte[0], bytes("value"));
        });
    }


    @Test
    void test06_emptyValue() {
        db.put(bytes("key1"), new byte[0]);
        assertArrayEquals(new byte[0], db.get(bytes("key1")));
    }

    @Test
    void test07_largeKey() {
        byte[] largeKey = new byte[(int) DEFAULT_MAX_KEY_SIZE + 1];
        assertThrows(IllegalArgumentException.class, () -> {
            db.put(largeKey, bytes("value"));
        });
    }


    @Test
    void test08_largeValue() {
        byte[] largeValue = new byte[(int) DEFAULT_MAX_VALUE_SIZE + 1];
        assertThrows(IllegalArgumentException.class, () -> {
            db.put(bytes("key"), largeValue);
        });
    }

    // 09 unicode key/value
    @Test
    void test09_unicodeKeyValue() {
        String key = "こんにちは";
        String value = "世界";
        db.put(bytes(key), bytes(value));
        assertEquals(value, string(db.get(bytes(key))));
    }


    @Test
    void test10_manyKeys() {
        for (int i = 0; i < 1000; i++) {
            db.put(bytes("many_key_" + i), bytes("many_value_" + i));
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals("many_value_" + i, string(db.get(bytes("many_key_" + i))));
        }
    }

    // 11 put → flush → get
    @Test
    void test11_putFlushGet() {
        db.put(bytes("key1"), bytes("value1"));
        db.close(); // Triggers flush

        db = TangoDB.open(config);
        assertEquals("value1", string(db.get(bytes("key1"))));
    }


    @Test
    void test12_putMultipleFlushesGet() {
        byte[] pad = new byte[1024]; // 1KB
        for (int i = 0; i < 200; i++) {
            db.put(bytes("flush_key" + i), pad); // 200KB total, triggers multiple flushes
        }

        for (int i = 0; i < 200; i++) {
            assertNotNull(db.get(bytes("flush_key" + i)));
        }
    }


    @Test
    void test13_overwriteAcrossFlush() {
        db.put(bytes("key1"), bytes("value1"));
        triggerFlush(); // Flushes key1 to SSTable
        db.put(bytes("key1"), bytes("value2")); // Overwrites in new memtable
        assertEquals("value2", string(db.get(bytes("key1"))));
    }


    @Test
    void test14_multipleKeysAcrossFlush() {
        db.put(bytes("key1"), bytes("value1"));
        triggerFlush();
        db.put(bytes("key2"), bytes("value2"));
        triggerFlush();
        db.put(bytes("key3"), bytes("value3"));

        assertEquals("value1", string(db.get(bytes("key1"))));
        assertEquals("value2", string(db.get(bytes("key2"))));
        assertEquals("value3", string(db.get(bytes("key3"))));
    }

    @Test
    void test15_largeDatasetAcrossFlush() {
        int numKeys = 5000;
        for (int i = 0; i < numKeys; i++) {
            db.put(bytes("large_key_" + i), bytes("large_val_" + i));
        }
        for (int i = 0; i < numKeys; i++) {
            assertEquals("large_val_" + i, string(db.get(bytes("large_key_" + i))));
        }
    }


    @Test
    void test16_reopenDatabaseGet() {
        db.put(bytes("key1"), bytes("value1"));
        db.close();

        db = TangoDB.open(config);
        assertEquals("value1", string(db.get(bytes("key1"))));
    }


    @Test
    void test17_reopenAfterMultipleSSTables() {
        byte[] pad = new byte[1024]; // 1KB
        for (int i = 0; i < 200; i++) {
            db.put(bytes("flush_key" + i), pad); // Triggers multiple flushes
        }
        db.close();

        db = TangoDB.open(config);
        for (int i = 0; i < 200; i++) {
            assertNotNull(db.get(bytes("flush_key" + i)));
        }
    }


    @Test
    void test18_dataRemainsAfterCloseOpen() {
        db.put(bytes("key1"), bytes("value1"));
        db.close();

        db = TangoDB.open(config);
        assertEquals("value1", string(db.get(bytes("key1"))));
        db.close();

        db = TangoDB.open(config);
        assertEquals("value1", string(db.get(bytes("key1"))));
    }


    @Test
    void test19_latestValueAfterReopen() {
        db.put(bytes("key1"), bytes("value1"));
        db.close();

        db = TangoDB.open(config);
        db.put(bytes("key1"), bytes("value2"));
        db.close();

        db = TangoDB.open(config);
        assertEquals("value2", string(db.get(bytes("key1"))));
    }


    @Test
    void test20_manyKeysAfterReopen() {
        int numKeys = 2000;
        for (int i = 0; i < numKeys; i++) {
            db.put(bytes("key" + i), bytes("value" + i));
        }
        db.close();

        db = TangoDB.open(config);
        for (int i = 0; i < numKeys; i++) {
            assertEquals("value" + i, string(db.get(bytes("key" + i))));
        }
    }

    private void waitForCompaction() {
        try {
            Thread.sleep(1000); // Wait 1 second for compaction to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    @Test
    void test21_sameKeyInTwoSSTables() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        assertEquals("val2", string(db.get(bytes("key1"))));
    }


    @Test
    void test22_sameKeyInThreeSSTables() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val3"));
        triggerFlush();
        assertEquals("val3", string(db.get(bytes("key1"))));
    }


    @Test
    void test23_latestValueWins() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        assertEquals("val2", string(db.get(bytes("key1"))));
    }


    @Test
    void test24_oldValueNeverReturned() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        assertNotEquals("val1", string(db.get(bytes("key1"))));
    }


    @Test
    void test25_interleavedKeys() {
        db.put(bytes("key1"), bytes("val1"));
        db.put(bytes("key2"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val1_new"));
        db.put(bytes("key3"), bytes("val3"));
        triggerFlush();
        assertEquals("val1_new", string(db.get(bytes("key1"))));
        assertEquals("val2", string(db.get(bytes("key2"))));
        assertEquals("val3", string(db.get(bytes("key3"))));
    }


    @Test
    void test26_overwriteBeforeFlush() {
        db.put(bytes("key1"), bytes("val1"));
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        assertEquals("val2", string(db.get(bytes("key1"))));
    }


    @Test
    void test27_overwriteAfterFlush() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        assertEquals("val2", string(db.get(bytes("key1"))));
    }


    @Test
    void test28_overwriteAcrossMultipleFlushes() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val3"));
        triggerFlush();
        assertEquals("val3", string(db.get(bytes("key1"))));
    }


    @Test
    void test29_manyVersionsOfSameKey() {
        for (int i = 0; i < 50; i++) {
            db.put(bytes("key1"), bytes("val" + i));
            if (i % 5 == 0) triggerFlush();
        }
        assertEquals("val49", string(db.get(bytes("key1"))));
    }


    @Test
    void test30_latestVersionSurvivesCompaction() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val3"));
        triggerFlush();

        waitForCompaction();
        assertEquals("val3", string(db.get(bytes("key1"))));
    }


    @Test
    void test31_compactionPreservesUniqueKeys() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key2"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key3"), bytes("val3"));
        triggerFlush();

        waitForCompaction();
        assertEquals("val1", string(db.get(bytes("key1"))));
        assertEquals("val2", string(db.get(bytes("key2"))));
        assertEquals("val3", string(db.get(bytes("key3"))));
    }


    @Test
    void test32_compactionPreservesLatestValues() {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val3"));
        triggerFlush();

        waitForCompaction();
        assertEquals("val3", string(db.get(bytes("key1"))));
    }


    @Test
    void test33_compactionHandlesDuplicateKeys() {
        for (int i = 0; i < 5; i++) {
            db.put(bytes("key_dup"), bytes("val_" + i));
            triggerFlush();
        }
        waitForCompaction();
        assertEquals("val_4", string(db.get(bytes("key_dup"))));
    }


    @Test
    void test34_compactionDoesNotLoseRecords() {
        for (int i = 0; i < 100; i++) {
            db.put(bytes("key" + i), bytes("val" + i));
        }
        triggerFlush();
        for (int i = 100; i < 200; i++) {
            db.put(bytes("key" + i), bytes("val" + i));
        }
        triggerFlush();
        for (int i = 200; i < 300; i++) {
            db.put(bytes("key" + i), bytes("val" + i));
        }
        triggerFlush();

        waitForCompaction();
        for (int i = 0; i < 300; i++) {
            assertEquals("val" + i, string(db.get(bytes("key" + i))));
        }
    }


    @Test
    void test35_multipleCompactions() {
        for (int i = 0; i < 10; i++) {
            db.put(bytes("key" + i), bytes("val" + i));
            triggerFlush();
        }
        waitForCompaction();
        for (int i = 0; i < 10; i++) {
            assertEquals("val" + i, string(db.get(bytes("key" + i))));
        }
    }


    @Test
    void test36_compactionWithManyKeys() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 1000; j++) {
                db.put(bytes("many_" + i + "_" + j), bytes("val"));
            }
            triggerFlush();
        }
        waitForCompaction();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 1000; j++) {
                assertNotNull(db.get(bytes("many_" + i + "_" + j)));
            }
        }
    }


    @Test
    void test37_compactionWithRepeatedUpdates() {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 100; j++) {
                db.put(bytes("repeat_" + j), bytes("val_" + i));
            }
            triggerFlush();
        }
        waitForCompaction();
        for (int j = 0; j < 100; j++) {
            assertEquals("val_4", string(db.get(bytes("repeat_" + j))));
        }
    }


    @Test
    void test38_readWhileCompactionOccurs() throws InterruptedException {
        db.put(bytes("key1"), bytes("val1"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val2"));
        triggerFlush();
        db.put(bytes("key1"), bytes("val3"));
        triggerFlush();

        for (int i = 0; i < 100; i++) {
            String val = string(db.get(bytes("key1")));
            assertTrue("val1".equals(val) || "val2".equals(val) || "val3".equals(val));
            Thread.sleep(5);
        }
    }


    @Test
    void test39_latestValueAfterCompaction() {
        test32_compactionPreservesLatestValues();
    }


    @Test
    void test40_databaseRemainsReadableAfterCompaction() {
        for (int i = 0; i < 5; i++) {
            db.put(bytes("key" + i), bytes("val" + i));
            triggerFlush();
        }
        waitForCompaction();
        db.put(bytes("new_key"), bytes("new_val"));
        assertEquals("new_val", string(db.get(bytes("new_key"))));
        assertEquals("val0", string(db.get(bytes("key0"))));
    }


    @Test
    void test41_1000Keys() {
        for (int i = 0; i < 1000; i++) db.put(bytes("k" + i), bytes("v" + i));
        for (int i = 0; i < 1000; i++) assertEquals("v" + i, string(db.get(bytes("k" + i))));
    }


    @Test
    void test42_10000Keys() {
        for (int i = 0; i < 10000; i++) db.put(bytes("k10k" + i), bytes("v" + i));
        for (int i = 0; i < 10000; i++) assertEquals("v" + i, string(db.get(bytes("k10k" + i))));
    }


    @Test
    void test43_repeatedUpdates() {
        for (int i = 0; i < 1000; i++) db.put(bytes("rep"), bytes("v" + i));
        assertEquals("v999", string(db.get(bytes("rep"))));
    }


    @Test
    void test44_randomKeys() {
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < 5000; i++) {
            db.put(bytes("rnd" + rnd.nextInt(10000)), bytes("val"));
        }
    }


    @Test
    void test45_randomPutGetWorkload() {
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < 5000; i++) {
            String k = "k" + rnd.nextInt(1000);
            if (rnd.nextBoolean()) {
                db.put(bytes(k), bytes("v"));
            } else {
                db.get(bytes(k));
            }
        }
    }


    @Test
    void test46_largeValues() {
        byte[] large = new byte[50 * 1024]; // 50KB, fits in memtable
        java.util.Arrays.fill(large, (byte) 'a');
        db.put(bytes("large_val"), large);
        assertArrayEquals(large, db.get(bytes("large_val")));
    }


    @Test
    void test47_longKeys() {
        byte[] longKey = new byte[3 * 1024]; // 3KB, fits in max key size 4KB
        java.util.Arrays.fill(longKey, (byte) 'k');
        db.put(longKey, bytes("val"));
        assertEquals("val", string(db.get(longKey)));
    }


    @Test
    void test48_mixedKeySizes() {
        db.put(bytes("k"), bytes("v"));
        byte[] k2 = new byte[100];
        db.put(k2, bytes("v2"));
        byte[] k3 = new byte[1000];
        db.put(k3, bytes("v3"));
        assertEquals("v", string(db.get(bytes("k"))));
        assertEquals("v2", string(db.get(k2)));
        assertEquals("v3", string(db.get(k3)));
    }


    @Test
    void test49_randomizedWorkloadVsHashMap() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        java.util.Random rnd = new java.util.Random(42);
        for (int i = 0; i < 5000; i++) {
            String k = "k" + rnd.nextInt(1000);
            String v = "v" + rnd.nextInt();
            map.put(k, v);
            db.put(bytes(k), bytes(v));
        }
        for (java.util.Map.Entry<String, String> entry : map.entrySet()) {
            assertEquals(entry.getValue(), string(db.get(bytes(entry.getKey()))));
        }
    }


    @Test
    void test50_concurrentPutGet() throws InterruptedException {
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
                assertNotNull(db.get(bytes("c_" + tId + "_" + i)));
            }
        }
    }
}
