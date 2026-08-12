package io.tango.engine.sstable;


import io.tango.common.constants.LookUpStatus;
import io.tango.common.io.BlockLayout;
import io.tango.common.util.SstDirPathResolver;
import io.tango.common.util.SstHeaderExtractor;
import io.tango.exception.TangoDBException;
import io.tango.model.SsTableResult;
import io.tango.model.SstPageMetadata;
import io.tango.model.SstWriteMetadata;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

class SstReader {

    private static final ValueLayout.OfInt INT_LAYOUT = ValueLayout.JAVA_INT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
    private static final ValueLayout.OfLong LONG_LAYOUT = ValueLayout.JAVA_LONG_UNALIGNED;
    private static final ValueLayout.OfByte BYTE_LAYOUT = ValueLayout.JAVA_BYTE;


    public static final int MAGIC = 0x47534442;
    public static final int VERSION = 1;


    SsTableResult readTable(byte[] searchKey, Path path) {

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {

            long totalBytesRead = 0;
            MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

            MemorySegment file = MemorySegment.ofBuffer(mapped);

            SstPageMetadata header = SstHeaderExtractor.extract(file);

            long offset = header.dataOffset();
            totalBytesRead = header.totalBytes();

            if (header.magic() != MAGIC) {
                throw new IllegalStateException("Invalid SSTable: " + path);
            }

            if (header.version() != VERSION) {
                throw new IllegalStateException("Unsupported SSTable version: " + header.version());
            }

            for (int record = 0; record < header.recordCount(); record++) {

                byte flag = file.get(BYTE_LAYOUT, offset);
                offset += Byte.BYTES;

                int keyLength = file.get(BlockLayout.INT_LAYOUT, offset);
                offset += Integer.BYTES;

                int valueLength = file.get(BlockLayout.INT_LAYOUT, offset);
                offset += Integer.BYTES;

                MemorySegment key = file.asSlice(offset, keyLength);
                offset += keyLength;

                MemorySegment value = file.asSlice(offset, valueLength);
                offset += valueLength;

                totalBytesRead = offset;

                if (!equals(key, searchKey)) {
                    continue;
                }

                if (flag != 0) {
                    return new SsTableResult(LookUpStatus.DELETED, null, totalBytesRead);
                }

                return new SsTableResult(LookUpStatus.FOUND, toByteArray(value), totalBytesRead);
            }

            return new SsTableResult(LookUpStatus.NOT_FOUND, null, totalBytesRead);

        } catch (IOException e) {

            throw new TangoDBException("Unable to read SSTable: " + path, e);
        }
    }


    public static byte[] toByteArray(MemorySegment segment) {

        return segment.toArray(ValueLayout.JAVA_BYTE);
    }

    SsTableResult read(byte[] searchKey, Path dataDir, List<SstWriteMetadata> ssTablesInfo) {

        for (int tableIndex = ssTablesInfo.size() - 1; tableIndex >= 0; tableIndex--) {

            SsTableResult result = readTable(searchKey, SstDirPathResolver.resolve(ssTablesInfo.get(tableIndex).getTableId(), dataDir));

            if (LookUpStatus.FOUND.equals(result.status())) {
                return result;
            }
        }

        return new SsTableResult(LookUpStatus.NOT_FOUND, null, 0L);
    }

    private static boolean equals(MemorySegment segment, byte[] key) {


        if (segment.byteSize() != key.length) {
            return false;
        }


        for (int i = 0; i < key.length; i++) {

            if (segment.get(ValueLayout.JAVA_BYTE, i) != key[i]) {

                return false;
            }
        }


        return true;
    }


}
