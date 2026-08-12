package io.tango.engine.memtable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class ArenaAllocator {

    private static final int ALIGNMENT = 4;

    private static final long MAX_MEMTABLE_SIZE = 64L * 1024 * 1024;

    private final ReentrantLock allocationLock = new ReentrantLock();

    private final Arena arena;
    private final long blockSize;
    private final int offsetBits;
    private long totalUsedBytes;

    private final List<ArenaBlock> blocks = new ArrayList<>();

    private ArenaBlock currentBlock;
    private int currentBlockId;

    private static final class ArenaBlock {

        final MemorySegment memory;
        long position;

        ArenaBlock(Arena arena, long blockSize) {
            this.memory = arena.allocate(blockSize);
            this.position = 0;
        }
    }

    ArenaAllocator(long blockSize) {

        this.arena = Arena.ofShared();
        this.blockSize = blockSize;

        // blockSize must be a power of 2 (64KB, 128KB)
        if ((blockSize & (blockSize - 1)) != 0) {
            throw new IllegalArgumentException("Block size must be a power of 2.");
        }

        this.offsetBits = Long.numberOfTrailingZeros(blockSize);

        this.currentBlock = new ArenaBlock(arena, blockSize);
        this.blocks.add(currentBlock);
        this.currentBlockId = 0;
    }

    public long allocate(long byteSize) {
        allocationLock.lock();
        try {

            currentBlock.position = align(currentBlock.position, ALIGNMENT);

            if (totalUsedBytes + byteSize > MAX_MEMTABLE_SIZE) {
                return -1;
            }

            if (currentBlock.position + byteSize > blockSize) {

                currentBlock = new ArenaBlock(arena, blockSize);
                blocks.add(currentBlock);
                currentBlockId++;
            }

            long offset = currentBlock.position;
            currentBlock.position += byteSize;
            totalUsedBytes += byteSize;

            return pack(currentBlockId, offset);
        } finally {
            allocationLock.unlock();
        }

    }

    public MemorySegment slice(long pointer, long size) {

        int blockId = unpackBlock(pointer);
        long offset = unpackOffset(pointer);

        return blocks.get(blockId).memory.asSlice(offset, size);
    }

    public MemorySegment slice(long pointer) {

        int blockId = unpackBlock(pointer);
        long offset = unpackOffset(pointer);

        return blocks.get(blockId).memory.asSlice(offset);
    }

    public long remaining() {
        return blockSize - currentBlock.position;
    }

    public long usedBytes() {

        long total = 0;

        for (ArenaBlock block : blocks) {
            total += block.position;
        }

        return total;
    }

    public int blockCount() {
        return blocks.size();
    }

    public void close() {
        arena.close();
    }

    private static long align(long value, long alignment) {

        long remainder = value % alignment;

        return remainder == 0 ? value : value + alignment - remainder;
    }

    private long pack(int blockId, long offset) {
        return ((long) blockId << offsetBits) | offset;
    }

    private int unpackBlock(long pointer) {
        return (int) (pointer >>> offsetBits);
    }

    private long unpackOffset(long pointer) {
        return pointer & ((1L << offsetBits) - 1);
    }
}
