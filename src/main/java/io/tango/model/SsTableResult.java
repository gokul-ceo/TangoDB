package io.tango.model;


import io.tango.common.constants.LookUpStatus;

public record SsTableResult(LookUpStatus status, byte[] value, Long totalBytesRead) {
}
