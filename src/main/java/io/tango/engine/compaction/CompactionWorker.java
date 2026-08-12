package io.tango.engine.compaction;


import io.tango.common.util.MemSegmentCompare;
import io.tango.common.util.SstDirPathResolver;
import io.tango.engine.sstable.SstRecordIterator;
import io.tango.engine.sstable.SstStreamWriter;
import io.tango.exception.TangoDBException;
import io.tango.model.CompactionResult;
import io.tango.model.SstPageMetadata;
import io.tango.model.SstRecord;

import java.io.IOException;
import java.nio.file.Path;

public class CompactionWorker {

    public CompactionResult runCompaction(
            long nextTableId,
            long selectedTableId1,
            long selectedTableId2,
            Path dirPath) {

        try (
                SstRecordIterator left =
                        new SstIterator(
                                SstDirPathResolver.resolve(
                                        selectedTableId1,
                                        dirPath));

                SstRecordIterator right =
                        new SstIterator(
                                SstDirPathResolver.resolve(
                                        selectedTableId2,
                                        dirPath));

                SstStreamWriter writer =
                        new SstStreamWriter(
                                nextTableId,
                                dirPath)
        ) {

            SstRecord l = left.hasNext() ? left.next() : null;
            SstRecord r = right.hasNext() ? right.next() : null;

            while (l != null && r != null) {

                int cmp = MemSegmentCompare.compare(
                        l.key(),
                        r.key());

                if (cmp < 0) {

                    writer.write(l);
                    l = left.hasNext() ? left.next() : null;

                } else if (cmp > 0) {

                    writer.write(r);
                    r = right.hasNext() ? right.next() : null;

                } else {

                    // Keep the record from the newer SST
                    writer.write(r);

                    l = left.hasNext() ? left.next() : null;
                    r = right.hasNext() ? right.next() : null;
                }
            }

            while (l != null) {

                writer.write(l);
                l = left.hasNext() ? left.next() : null;
            }

            while (r != null) {

                writer.write(r);
                r = right.hasNext() ? right.next() : null;
            }

            SstPageMetadata metadata = writer.finish();

            return new CompactionResult(
                    metadata.tableId(),
                    2
            );

        } catch (IOException e) {

            throw new TangoDBException(
                    "Failed to compact SSTables",
                    e);
        }
    }
}
