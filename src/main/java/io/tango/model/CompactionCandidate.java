package io.tango.model;

public record CompactionCandidate(
        long leftTableId,
        long rightTableId
) {
}
