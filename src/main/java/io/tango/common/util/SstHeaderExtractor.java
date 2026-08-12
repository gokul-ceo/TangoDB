package io.tango.common.util;

import io.tango.model.SstPageMetadata;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public class SstHeaderExtractor {

    private static final ValueLayout.OfInt INT_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfLong LONG_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED;
    private static final ValueLayout.OfByte BYTE_LAYOUT = ValueLayout.JAVA_BYTE;

    public static SstPageMetadata extract(MemorySegment file) {

        long offset = 0;

        int magic = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        int version = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        long tableId = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        long createdTime = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        int recordCount = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        long fileSize = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        long dataOffset = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        long bloomFilterOffset = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        long indexOffset = file.get(LONG_LAYOUT, offset);
        offset += Long.BYTES;

        int tombstoneCount = file.get(INT_LAYOUT, offset);
        offset += Integer.BYTES;

        int flags = file.get(INT_LAYOUT, offset);

        int level = file.get(INT_LAYOUT, offset);

        return new SstPageMetadata(
                magic,
                version,
                tableId,
                createdTime,
                recordCount,
                fileSize,
                dataOffset,
                bloomFilterOffset,
                indexOffset,
                tombstoneCount,
                flags,
                offset, // totalBytesRead
                level
        );
    }
}
