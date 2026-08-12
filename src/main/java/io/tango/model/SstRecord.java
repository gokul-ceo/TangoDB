package io.tango.model;

import java.lang.foreign.MemorySegment;

public record SstRecord(byte flag, MemorySegment key, MemorySegment value) {
}
