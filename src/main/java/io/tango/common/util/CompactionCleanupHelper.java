package io.tango.common.util;

import io.tango.exception.TangoDBException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CompactionCleanupHelper {

    private CompactionCleanupHelper() {
    }

    public static void cleanup(
            long leftTableId,
            long rightTableId,
            Path dir) {

        delete(leftTableId, dir);
        delete(rightTableId, dir);
    }

    private static void delete(long tableId, Path dir) {
        Path file = SstDirPathResolver.resolve(tableId, dir);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new TangoDBException(
                    "Failed to delete compacted SST: " + tableId,
                    e
            );
        }
    }
}
