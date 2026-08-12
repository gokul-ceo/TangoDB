package io.tango.model;

import java.lang.foreign.MemorySegment;

public record SstPageMetadata(
        int magic,
        int version,
        long tableId,
        long createdTime,
        int recordCount,
        long fileSize,
        long dataOffset,
        long bloomFilterOffset,
        long indexOffset,
        int tombstoneCount,
        int flags,
        long totalBytes,
        int level
) {}
