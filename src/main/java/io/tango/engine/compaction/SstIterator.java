package io.tango.engine.compaction;


import io.tango.common.util.SstHeaderExtractor;
import io.tango.engine.sstable.SstRecordIterator;
import io.tango.model.SstPageMetadata;
import io.tango.model.SstRecord;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SstIterator implements SstRecordIterator {

    private static final ValueLayout.OfInt INT_LAYOUT =
            ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);

    private static final ValueLayout.OfByte BYTE_LAYOUT =
            ValueLayout.JAVA_BYTE;

    private final FileChannel channel;
    private final Arena arena;
    private final MemorySegment file;

    private final int recordCount;

    private long offset;
    private int currentRecord;

    public SstIterator(Path path) throws IOException {

        this.channel = FileChannel.open(path, StandardOpenOption.READ);
        this.arena = Arena.ofConfined();

        long fileSize = channel.size();

        this.file = arena.allocate(fileSize);

        ByteBuffer buffer = file.asByteBuffer();

        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                break;
            }
        }

        buffer.flip();

        SstPageMetadata header = SstHeaderExtractor.extract(file);

        this.recordCount = header.recordCount();
        this.offset = header.dataOffset();
        this.currentRecord = 0;
    }

    @Override
    public boolean hasNext() {
        return currentRecord < recordCount;
    }

    @Override
    public SstRecord next() {

        if (!hasNext()) {
            return null;
        }

        byte flag = file.get(BYTE_LAYOUT, offset);
        offset += Byte.BYTES;

        int keyLength = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        int valueLength = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        MemorySegment key = file.asSlice(offset, keyLength);
        offset += keyLength;

        MemorySegment value = file.asSlice(offset, valueLength);
        offset += valueLength;

        currentRecord++;

        return new SstRecord(flag, key, value);
    }

    @Override
    public void close() {

        try {
            arena.close();
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close SST iterator", e);
        }
    }
}
