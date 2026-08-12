package io.tango.model;

public record CompactionResult(Long newTableId,int numberOfmergedFiles) {
}
