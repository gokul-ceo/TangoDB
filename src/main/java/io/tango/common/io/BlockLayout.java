package io.tango.common.io;

import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public final class BlockLayout {

    public static final long FLAGS_OFFSET = 0;
    public static final long KEY_LENGTH_OFFSET = 1;
    public static final long VALUE_LENGTH_OFFSET = 5;
    public static final long HEADER_SIZE = 9;
    public static final ValueLayout.OfInt INT_LAYOUT =
            ValueLayout.JAVA_INT_UNALIGNED
                    .withOrder(ByteOrder.LITTLE_ENDIAN);
}
