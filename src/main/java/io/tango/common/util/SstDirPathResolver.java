package io.tango.common.util;

import java.nio.file.Path;

public class SstDirPathResolver {

    public static Path resolve(Long tableId, Path dataDir) {
        return dataDir.resolve(String.format("%06d.sgdb", tableId));
    }
}
