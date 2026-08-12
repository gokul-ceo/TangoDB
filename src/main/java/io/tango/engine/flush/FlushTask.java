package io.tango.engine.flush;

sealed public interface FlushTask permits FlushTable, ShutdownTask {
}
