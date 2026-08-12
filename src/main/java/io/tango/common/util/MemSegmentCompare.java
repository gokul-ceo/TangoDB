package io.tango.common.util;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class MemSegmentCompare {

    public static int compare(
            MemorySegment left,
            MemorySegment right
    ) {
        long leftLength = left.byteSize();
        long rightLength = right.byteSize();

        long min = Math.min(leftLength, rightLength);

        for (long i = 0; i < min; i++) {
            int a = Byte.toUnsignedInt(left.get(ValueLayout.JAVA_BYTE, i));
            int b = Byte.toUnsignedInt(right.get(ValueLayout.JAVA_BYTE, i));
            if (a != b) {
                return Integer.compare(a, b);
            }
        }

        return Long.compare(leftLength, rightLength);
    }
}
