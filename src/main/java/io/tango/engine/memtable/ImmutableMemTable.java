package io.tango.engine.memtable;



import io.tango.common.constants.LookUpStatus;
import io.tango.common.io.BlockLayout;
import io.tango.model.MemTableResult;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

public class ImmutableMemTable {

    private final ArenaAllocator store;
    private final ConcurrentSkipListMap<ByteArrayWrapper, Long> index;
    private long previousOffset = 0;

    public int size() {
        return index.size();
    }

    ImmutableMemTable(ArenaAllocator store, ConcurrentSkipListMap<ByteArrayWrapper, Long> index) {
        this.store = store;
        this.index = index;
    }

    ;

    public MemTableResult get(byte[] key) {

        long totalBytesRead = 0L;
        long offset = index.getOrDefault(new ByteArrayWrapper(key), -1L);
        if (offset == -1L) {
            return new MemTableResult(LookUpStatus.NOT_FOUND, null, totalBytesRead);
        }
        MemorySegment entry = this.store.slice(offset);

        byte flags = entry.get(ValueLayout.JAVA_BYTE, BlockLayout.FLAGS_OFFSET);

        int keyLength = entry.get(BlockLayout.INT_LAYOUT, BlockLayout.KEY_LENGTH_OFFSET);
        int valueLength = entry.get(BlockLayout.INT_LAYOUT, BlockLayout.VALUE_LENGTH_OFFSET);

        totalBytesRead = BlockLayout.HEADER_SIZE + keyLength + valueLength;

        // if flag is 1 then the current block is marked as tombstone i.e) marked as
        // deleted
        if (flags == 1) {
            return new MemTableResult(LookUpStatus.FOUND_TOMBSTONE, null, totalBytesRead);
        }

        byte[] value = new byte[valueLength];
        MemorySegment.copy(entry, ValueLayout.JAVA_BYTE, BlockLayout.HEADER_SIZE + keyLength, value, 0, valueLength);
        return new MemTableResult(LookUpStatus.FOUND, value, totalBytesRead);
    }

    public Iterator<Long> offsets() {
        return this.index.values().iterator();
    }

    public MemorySegment record(long offset) {
        MemorySegment segment = this.store.slice(offset);

        int keyLength = segment.get(BlockLayout.INT_LAYOUT, BlockLayout.KEY_LENGTH_OFFSET);

        int valueLength = segment.get(BlockLayout.INT_LAYOUT, BlockLayout.VALUE_LENGTH_OFFSET);

        long size = BlockLayout.HEADER_SIZE + keyLength + valueLength;

        return this.store.slice(offset, size);
    }

    public void close() {
        store.close();

    }


}
