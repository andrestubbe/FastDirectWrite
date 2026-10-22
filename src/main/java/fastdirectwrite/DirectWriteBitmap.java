package fastdirectwrite;

import java.lang.foreign.MemorySegment;

public record DirectWriteBitmap(
        int width,
        int height,
        MemorySegment pixels
) {
    public int getPixel(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) return 0;
        return pixels.get(java.lang.foreign.ValueLayout.JAVA_INT, (long) (y * width + x) * 4);
    }
}
