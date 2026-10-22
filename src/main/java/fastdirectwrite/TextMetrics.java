package fastdirectwrite;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public record TextMetrics(
        float width,
        float height,
        float widthIncludingTrailingWhitespace,
        float layoutWidth,
        float layoutHeight,
        int lineCount
) {
    public static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_FLOAT.withName("width"),
            ValueLayout.JAVA_FLOAT.withName("height"),
            ValueLayout.JAVA_FLOAT.withName("widthIncludingTrailingWhitespace"),
            ValueLayout.JAVA_FLOAT.withName("layoutWidth"),
            ValueLayout.JAVA_FLOAT.withName("layoutHeight"),
            ValueLayout.JAVA_INT.withName("lineCount")
    );

    public static TextMetrics fromSegment(MemorySegment seg) {
        return new TextMetrics(
                seg.get(ValueLayout.JAVA_FLOAT, 0),
                seg.get(ValueLayout.JAVA_FLOAT, 4),
                seg.get(ValueLayout.JAVA_FLOAT, 8),
                seg.get(ValueLayout.JAVA_FLOAT, 12),
                seg.get(ValueLayout.JAVA_FLOAT, 16),
                seg.get(ValueLayout.JAVA_INT, 20)
        );
    }
}
