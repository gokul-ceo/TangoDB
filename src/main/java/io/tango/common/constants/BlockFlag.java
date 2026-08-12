package io.tango.common.constants;

public enum BlockFlag {

    DEFAULT((byte) 0),
    TOMBSTONE((byte) 1);

    private final byte code;

    BlockFlag(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
