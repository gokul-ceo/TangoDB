package io.tango.engine.memtable;

import java.util.Arrays;

public class ByteArrayWrapper implements Comparable<ByteArrayWrapper> {

    private final byte[] bytes;

    public ByteArrayWrapper(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] bytes() {
        return bytes;
    }

    @Override
    public int compareTo(ByteArrayWrapper other) {
        return Arrays.compareUnsigned(this.bytes, other.bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ByteArrayWrapper other)) {
            return false;
        }
        return Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }
}
