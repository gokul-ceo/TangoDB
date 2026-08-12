package io.tango.model;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class StorageMetricsData {

    private final LongAdder reads = new LongAdder();
    private final LongAdder writes = new LongAdder();

    private final LongAdder bytesRead = new LongAdder();
    private final LongAdder bytesWritten = new LongAdder();

    private final LongAdder bloomHits = new LongAdder();
    private final LongAdder bloomMisses = new LongAdder();

    private final LongAdder compactions = new LongAdder();
    private final LongAdder tombstones = new LongAdder();

    private final LongAdder totalLookups = new LongAdder();
    private final LongAdder totalLookupSstVisited = new LongAdder();

    private final AtomicInteger l0SstCount = new AtomicInteger();
    private final AtomicInteger flushQueueDepth = new AtomicInteger();

    private final AtomicLong lastAccessTime = new AtomicLong();

    public void incrementL0SstCount(){
        l0SstCount.incrementAndGet();
    }

    public void recordRead(long bytes) {
        reads.increment();
        bytesRead.add(bytes);
        lastAccessTime.set(System.currentTimeMillis());
    }

    public void recordWrite(long bytes) {
        writes.increment();
        bytesWritten.add(bytes);
        lastAccessTime.set(System.currentTimeMillis());
    }

    public void recordLookup(int sstVisited) {
        totalLookups.increment();
        totalLookupSstVisited.add(sstVisited);
        lastAccessTime.set(System.currentTimeMillis());
    }

    public void recordBloomHit() {
        bloomHits.increment();
    }

    public void recordBloomMiss() {
        bloomMisses.increment();
    }

    public void recordCompaction() {
        compactions.increment();
    }

    public void recordTombstone() {
        tombstones.increment();
    }

    public void setL0SstCount(int count) {
        l0SstCount.set(count);
    }

    public void setFlushQueueDepth(int depth) {
        flushQueueDepth.set(depth);
    }

    public long getReads() {
        return reads.sum();
    }

    public long getWrites() {
        return writes.sum();
    }

    public long getBytesRead() {
        return bytesRead.sum();
    }

    public long getBytesWritten() {
        return bytesWritten.sum();
    }

    public long getBloomHits() {
        return bloomHits.sum();
    }

    public long getBloomMisses() {
        return bloomMisses.sum();
    }

    public long getCompactions() {
        return compactions.sum();
    }

    public long getTombstones() {
        return tombstones.sum();
    }

    public long getTotalLookups() {
        return totalLookups.sum();
    }

    public long getTotalLookupSstVisited() {
        return totalLookupSstVisited.sum();
    }

    public int getL0SstCount() {
        return l0SstCount.get();
    }

    public int getFlushQueueDepth() {
        return flushQueueDepth.get();
    }

    public long getLastAccessTime() {
        return lastAccessTime.get();
    }


    public double getFalsePositiveRate() {
        long total = bloomHits.sum() + bloomMisses.sum();
        return total == 0 ? 0.0 : (double) bloomMisses.sum() / total;
    }

    public double getAverageSstVisitedPerLookup() {
        long lookups = totalLookups.sum();
        return lookups == 0
                ? 0.0
                : (double) totalLookupSstVisited.sum() / lookups;
    }
}
