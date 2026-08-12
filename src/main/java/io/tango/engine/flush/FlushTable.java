package io.tango.engine.flush;

import io.tango.engine.memtable.ImmutableMemTable;

public record FlushTable(ImmutableMemTable table) implements FlushTask {
}
