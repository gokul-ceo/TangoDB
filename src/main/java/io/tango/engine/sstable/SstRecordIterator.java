package io.tango.engine.sstable;


import io.tango.model.SstRecord;

public interface SstRecordIterator extends AutoCloseable {

    boolean hasNext();

    SstRecord next();

    @Override
    void close();
}
