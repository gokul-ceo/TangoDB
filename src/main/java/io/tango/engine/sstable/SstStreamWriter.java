package io.tango.engine.sstable;


import io.tango.exception.TangoDBException;
import io.tango.model.SstPageMetadata;
import io.tango.model.SstRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class SstStreamWriter implements AutoCloseable {

    public static final int HEADER_SIZE = 256;
    public static final int DATA_START_OFFSET = HEADER_SIZE;

    public static final int MAGIC = 0x47534442;
    public static final int VERSION = 1;

    private static final int RECORD_HEADER_SIZE = Byte.BYTES + Integer.BYTES + Integer.BYTES;

    private final FileChannel channel;
    private final Path file;
    private final long tableId;
    private final long createdTime;

    private final ByteBuffer recordHeader = ByteBuffer.allocate(RECORD_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

    private final ByteBuffer pageHeader = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN);

    private int recordCount;
    private int tombstoneCount;

    private long totalBytesWritten = 0;

    public SstStreamWriter(long nextTableId, Path directory) throws IOException {

        Files.createDirectories(directory);

        this.tableId = nextTableId;
        this.createdTime = System.currentTimeMillis();

        this.file = directory.resolve(String.format("%06d.sgdb", tableId));

        this.channel = FileChannel.open(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        channel.position(DATA_START_OFFSET);
    }

    public void write(SstRecord record) {

        try {

            recordHeader.clear();

            recordHeader.put(record.flag());
            recordHeader.putInt((int) record.key().byteSize());
            recordHeader.putInt((int) record.value().byteSize());

            recordHeader.flip();

            while (recordHeader.hasRemaining()) {
                totalBytesWritten += Byte.BYTES + Integer.BYTES + Integer.BYTES;
                channel.write(recordHeader);
            }

            ByteBuffer keyBuffer = record.key().asByteBuffer();

            while (keyBuffer.hasRemaining()) {
                totalBytesWritten += record.key().byteSize();
                channel.write(keyBuffer);
            }

            ByteBuffer valueBuffer = record.value().asByteBuffer();

            while (valueBuffer.hasRemaining()) {
                totalBytesWritten += record.value().byteSize();
                channel.write(valueBuffer);
            }

            recordCount++;

            if (record.flag() != 0) {
                tombstoneCount++;
            }

        } catch (IOException e) {
            throw new TangoDBException("Failed to write SST record", e);
        }
    }

    public SstPageMetadata finish() {

        try {

            long fileSize = channel.position();

            pageHeader.clear();

            pageHeader.putInt(MAGIC);
            pageHeader.putInt(VERSION);

            pageHeader.putLong(tableId);
            pageHeader.putLong(createdTime);

            pageHeader.putInt(recordCount);

            pageHeader.putLong(fileSize);

            pageHeader.putLong(DATA_START_OFFSET);
            pageHeader.putLong(0L); // Bloom filter offset
            pageHeader.putLong(0L); // Index offset

            pageHeader.putInt(tombstoneCount);
            pageHeader.putInt(0);   // Flags
            pageHeader.putInt(1); // Compaction level indicator

            pageHeader.position(HEADER_SIZE);
            pageHeader.flip();

            channel.position(0);

            while (pageHeader.hasRemaining()) {
                channel.write(pageHeader);
            }

            channel.force(true);
            return new SstPageMetadata(MAGIC, VERSION, tableId, createdTime, recordCount, fileSize, DATA_START_OFFSET, 0L, 0L, tombstoneCount, 0, totalBytesWritten, 0);

        } catch (IOException e) {
            throw new TangoDBException("Failed to finalize SST", e);
        }
    }

    @Override
    public void close() {

        try {
            channel.close();
        } catch (IOException e) {
            throw new TangoDBException("Failed to close SST writer", e);
        }
    }
}
