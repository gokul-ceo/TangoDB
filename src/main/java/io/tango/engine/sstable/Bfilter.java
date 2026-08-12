package io.tango.engine.sstable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class Bfilter {

    private static final double LN_2 = Math.log(2.0);

    private final MemorySegment bits;
    private final long bitCount;
    private final int hashCount;

    public Bfilter(
            Arena arena,
            int expectedEntries,
            double falsePositiveRate) {

        if (expectedEntries <= 0) {
            throw new IllegalArgumentException(
                    "expectedEntries must be greater than 0"
            );
        }

        if (falsePositiveRate <= 0.0 ||
                falsePositiveRate >= 1.0) {
            throw new IllegalArgumentException(
                    "falsePositiveRate must be between 0 and 1"
            );
        }

        this.bitCount = calculateBitCount(
                expectedEntries,
                falsePositiveRate
        );

        this.hashCount = calculateHashCount(
                bitCount,
                expectedEntries
        );

        long byteCount = (bitCount + 7) >>> 3;
        this.bits = arena.allocate(
                byteCount,
                1
        );


    }


    public void add(byte[] key) {

        if (key == null) {
            throw new IllegalArgumentException(
                    "key cannot be null"
            );
        }

        long hash1 = hash64(key);
        long hash2 = secondaryHash(hash1);

        for (int i = 0; i < hashCount; i++) {

            long hash =
                    hash1 + ((long) i * hash2);

            long position =
                    Long.remainderUnsigned(
                            hash,
                            bitCount
                    );

            setBit(position);
        }
    }

    public boolean mightContain(byte[] key) {

        if (key == null) {
            throw new IllegalArgumentException(
                    "key cannot be null"
            );
        }

        long hash1 = hash64(key);
        long hash2 = secondaryHash(hash1);

        for (int i = 0; i < hashCount; i++) {

            long hash =
                    hash1 + ((long) i * hash2);

            long position =
                    Long.remainderUnsigned(
                            hash,
                            bitCount
                    );

            if (!getBit(position)) {
                return false;
            }
        }

        return true;
    }

    private void setBit(long position) {

        long byteIndex = position >>> 3;
        int bitIndex = (int) (position & 7);

        byte current =
                bits.get(
                        ValueLayout.JAVA_BYTE,
                        byteIndex
                );

        byte updated =
                (byte) (
                        current |
                                (1 << bitIndex)
                );

        bits.set(
                ValueLayout.JAVA_BYTE,
                byteIndex,
                updated
        );
    }

    private boolean getBit(long position) {

        long byteIndex = position >>> 3;
        int bitIndex = (int) (position & 7);

        byte current =
                bits.get(
                        ValueLayout.JAVA_BYTE,
                        byteIndex
                );

        return (current & (1 << bitIndex)) != 0;
    }


    public MemorySegment segment() {
        return bits;
    }

    public long bitCount() {
        return bitCount;
    }

    public int hashCount() {
        return hashCount;
    }

    public long byteCount() {
        return bits.byteSize();
    }


    private static long calculateBitCount(
            long entries,
            double falsePositiveRate) {

        double bits =
                -(entries * Math.log(falsePositiveRate))
                        / (LN_2 * LN_2);

        return Math.max(
                64,
                (long) Math.ceil(bits)
        );
    }

    private static int calculateHashCount(
            long bits,
            long entries) {

        int hashes =
                (int) Math.round(
                        ((double) bits / entries) * LN_2
                );

        return Math.max(1, hashes);
    }

    private static long hash64(byte[] data) {

        long hash = 0xcbf29ce484222325L;

        for (byte b : data) {

            hash ^= (b & 0xffL);

            hash *= 0x100000001b3L;
        }

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdl;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;

        return hash;
    }


    private static long secondaryHash(long hash) {

        hash ^= hash >>> 33;
        hash *= 0xff51afd7ed558ccdL;
        hash ^= hash >>> 33;
        hash *= 0xc4ceb9fe1a85ec53L;
        hash ^= hash >>> 33;

        return hash == 0
                ? 0x9E3779B97F4A7C15L
                : hash;
    }


}
