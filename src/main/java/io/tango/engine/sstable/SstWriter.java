package io.tango.engine.sstable;

import io.tango.engine.memtable.ImmutableMemTable;
import io.tango.exception.TangoDBException;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

import static io.tango.engine.sstable.SstManager.DATA_START_OFFSET;
import static io.tango.engine.sstable.SstManager.HEADER_SIZE;

public final class SstWriter {

    public static final int MAGIC = 0x47534442;
    public static final int VERSION = 1;

    private static final int WRITE_BUFFER_SIZE = 8 * 1024 * 1024;

    private static final String SST_EXTENSION = ".sgdb";

    ByteBuffer stagingBuffer =
            ByteBuffer.allocateDirect(WRITE_BUFFER_SIZE);

    private final ByteBuffer headerBuffer =
            ByteBuffer.allocate(HEADER_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN);

    public void write(
            long tableId,
            ImmutableMemTable table,
            Path directory) {

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new TangoDBException(
                    "Unable to create SST directory: " + directory,
                    e
            );
        }

        Path file = directory.resolve(
                formatFileName(tableId)
        );

        try (FileChannel channel = FileChannel.open(
                file,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {


            channel.position(DATA_START_OFFSET);

            long dataBytes = writeRecords(
                    channel,
                    table
            );

            flushStagingBuffer(channel);

            final long fileSize =
                    DATA_START_OFFSET + dataBytes;

            writeHeader(
                    channel,
                    tableId,
                    table.size(),
                    fileSize
            );
            channel.force(true);

        } catch (IOException e) {
            throw new TangoDBException(
                    "Failed to write SSTable: " + file,
                    e
            );
        }
    }

    private long writeRecords(
            FileChannel channel,
            ImmutableMemTable table) throws IOException {



        Iterator<Long> iterator = table.offsets();

        long totalBytes = 0;

        while (iterator.hasNext()) {

            final long offset = iterator.next();

            final MemorySegment record =
                    table.record(offset);

            final ByteBuffer source =
                    record.asByteBuffer();

            final int recordSize = source.remaining();
            if (recordSize >= WRITE_BUFFER_SIZE
                    && stagingBuffer.position() == 0) {

                writeFully(channel, source);

                totalBytes += recordSize;
                continue;
            }

            while (source.hasRemaining()) {

                if (!stagingBuffer.hasRemaining()) {
                    flushStagingBuffer(channel);
                }

                int bytesToCopy = Math.min(
                        source.remaining(),
                        stagingBuffer.remaining()
                );


                final int originalLimit =
                        source.limit();

                source.limit(
                        source.position() + bytesToCopy
                );

                stagingBuffer.put(source);

                source.limit(originalLimit);
            }
            totalBytes += recordSize;
        }
        return totalBytes;
    }

    private void flush(
            FileChannel channel,
            ByteBuffer buffer) throws IOException {

        buffer.flip();

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        buffer.clear();
    }



    private void writeHeader(
            FileChannel channel,
            long tableId,
            int recordCount,
            long fileSize) throws IOException {

        headerBuffer.clear();
        headerBuffer.putInt(MAGIC);
        headerBuffer.putInt(VERSION);

        headerBuffer.putLong(tableId);

        headerBuffer.putLong(
                System.currentTimeMillis()
        );

        headerBuffer.putInt(recordCount);

        headerBuffer.putLong(fileSize);

        headerBuffer.putLong(DATA_START_OFFSET);

        headerBuffer.putLong(0L); // Bloom filter offset
        headerBuffer.putLong(0L); // Index offset

        headerBuffer.putInt(0);   // Tombstone count
        headerBuffer.putInt(0);   // Flags
        headerBuffer.putInt(1);   // Level

        while (headerBuffer.position() < HEADER_SIZE) {
            headerBuffer.put((byte) 0);
        }


        headerBuffer.flip();


        channel.position(0);

        writeFully(
                channel,
                headerBuffer
        );
    }

    private void flushStagingBuffer(
            FileChannel channel) throws IOException {

        if (stagingBuffer.position() == 0) {
            return;
        }

        stagingBuffer.flip();

        writeFully(
                channel,
                stagingBuffer
        );

        stagingBuffer.clear();
    }

    private static void writeFully(
            FileChannel channel,
            ByteBuffer buffer) throws IOException {

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    private static String formatFileName(long tableId) {

        return String.format(
                "%06d%s",
                tableId,
                SST_EXTENSION
        );
    }
}