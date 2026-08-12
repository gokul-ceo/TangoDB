package io.tango.model;


import io.tango.common.constants.LookUpStatus;

public record MemTableResult(LookUpStatus status, byte[] value, long totalBytesRead) {

}
