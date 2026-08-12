package io.tango.engine.memtable;


import io.tango.common.constants.BlockFlag;
import io.tango.common.constants.DefaultConstants;
import io.tango.common.constants.LookUpStatus;
import io.tango.common.io.BlockLayout;
import io.tango.model.MemTableResult;
import io.tango.api.TangoConfig;
import io.tango.model.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemTable {

    private static final Logger log =
            LoggerFactory.getLogger(MemTable.class);

    private final ArenaAllocator store;
    private final ConcurrentSkipListMap<ByteArrayWrapper, Long> index;
    private boolean isFreezed;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    public MemTable(TangoConfig config) {
        this.store = new ArenaAllocator(config.getArenaBlockSize());
        this.index = new ConcurrentSkipListMap<>();
        isFreezed = false;
    }

    ;

    public ImmutableMemTable freeze() {

        lock.writeLock().lock();
        try {
            isFreezed = true;
            return new ImmutableMemTable(store, index);
        } finally {
            lock.writeLock().unlock();
        }

    }


    public WriteResult put(byte[] key, byte[] value, BlockFlag flag) {

        lock.readLock().lock();
        try {
            if (isFreezed) {
                throw new IllegalStateException("MemTable is immutable.");
            }

            long lengthOfTheBlock = BlockLayout.HEADER_SIZE + key.length + value.length;
            long totalWrittenBytes = lengthOfTheBlock;
            long offset = this.store.allocate(lengthOfTheBlock);
            if (offset == -1) {
                return new WriteResult(false, 0L);
            }


            MemorySegment memSeg = this.store.slice(offset, lengthOfTheBlock);
            memSeg.set(ValueLayout.JAVA_BYTE, BlockLayout.FLAGS_OFFSET, flag.getCode());
            memSeg.set(BlockLayout.INT_LAYOUT,
                    BlockLayout.KEY_LENGTH_OFFSET,
                    key.length);
            memSeg.set(BlockLayout.INT_LAYOUT,
                    BlockLayout.VALUE_LENGTH_OFFSET,
                    value.length);

            long keyPos = BlockLayout.HEADER_SIZE;
            for (byte b : key) {
                memSeg.set(ValueLayout.JAVA_BYTE, keyPos++, b);

            }
            long valuePos = BlockLayout.HEADER_SIZE + key.length;
            for (byte b : value) {
                memSeg.set(ValueLayout.JAVA_BYTE, valuePos++, b);

            }

            // updating the current offset to skip-list after memory insertion
            ByteArrayWrapper wrappedKey = new ByteArrayWrapper(key);
            index.put(wrappedKey, offset);
            return new WriteResult(true, totalWrittenBytes);
        } finally {
            lock.readLock().unlock();
        }

    }

    ;

    public MemTableResult get(byte[] key) {
        long offset = index.getOrDefault(new ByteArrayWrapper(key), -1L);
        long totalBytesRead = 0;
        if (offset == -1L) {
            return new MemTableResult(LookUpStatus.NOT_FOUND, null, 0L);
        }
        MemorySegment entry = this.store.slice(offset);

        byte flags = entry.get(
                ValueLayout.JAVA_BYTE,
                BlockLayout.FLAGS_OFFSET
        );

        int keyLength = entry.get(
                ValueLayout.JAVA_INT_UNALIGNED,
                BlockLayout.KEY_LENGTH_OFFSET
        );

        int valueLength = entry.get(
                ValueLayout.JAVA_INT_UNALIGNED,
                BlockLayout.VALUE_LENGTH_OFFSET
        );

        totalBytesRead =
                BlockLayout.HEADER_SIZE
                        + keyLength
                        + valueLength;


        // if flag is 1 then the current block is marked as tombstone i.e) marked as deleted
        if (flags == 1) {
            return new MemTableResult(LookUpStatus.FOUND_TOMBSTONE, null, totalBytesRead);
        }


        byte[] value = new byte[valueLength];
        long valueOffset = BlockLayout.HEADER_SIZE + (long) keyLength;
        MemorySegment.copy(entry, ValueLayout.JAVA_BYTE, BlockLayout.HEADER_SIZE + keyLength, value, 0, valueLength);
        return new MemTableResult(LookUpStatus.FOUND, value, totalBytesRead);
    }

    ;

    public void remove(byte[] key) {
        MemTableResult result = get(key);
        if (LookUpStatus.FOUND.equals(result.status())) {
            put(key, result.value(), BlockFlag.TOMBSTONE);
        }

    }

    private boolean isFull() {
        return store.remaining() < DefaultConstants.DEFAULT_ARENA_SIZE;
    }

    ;

    public void close() {
        store.close();
        ;
    }


}
