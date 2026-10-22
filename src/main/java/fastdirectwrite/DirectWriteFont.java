package fastdirectwrite;

import java.lang.foreign.MemorySegment;

public record DirectWriteFont(
        MemorySegment handle,
        String familyName,
        float size,
        FontWeight weight,
        FontStyle style
) implements AutoCloseable {

    @Override
    public void close() {
        FastDirectWrite.destroyFont(this);
    }
}
