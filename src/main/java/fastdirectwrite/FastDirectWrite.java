package fastdirectwrite;

import fastcore.FastCore;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;

/**
 * <h1>FastDirectWrite</h1>
 * High-performance, zero-AWT Microsoft DirectWrite Typography Engine for FastJava.
 * Powered by direct Win32/DirectWrite ABI downcalls via Java 21+ FFM (Foreign Function & Memory API).
 */
public final class FastDirectWrite implements AutoCloseable {

    private static final MethodHandle MH_CREATE;
    private static final MethodHandle MH_DESTROY;
    private static final MethodHandle MH_CREATE_FONT;
    private static final MethodHandle MH_DESTROY_FONT;
    private static final MethodHandle MH_MEASURE_TEXT;
    private static final MethodHandle MH_RENDER_TEXT;

    static {
        try {
            Arena arena = Arena.global();
            MH_CREATE = FastCore.lookupFunction("fastdirectwrite", "fastdw_create",
                    FunctionDescriptor.of(ValueLayout.ADDRESS),
                    FastDirectWrite.class, arena);

            MH_DESTROY = FastCore.lookupFunction("fastdirectwrite", "fastdw_destroy",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    FastDirectWrite.class, arena);

            MH_CREATE_FONT = FastCore.lookupFunction("fastdirectwrite", "fastdw_create_font",
                    FunctionDescriptor.of(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_FLOAT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    ),
                    FastDirectWrite.class, arena);

            MH_DESTROY_FONT = FastCore.lookupFunction("fastdirectwrite", "fastdw_destroy_font",
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                    FastDirectWrite.class, arena);

            MH_MEASURE_TEXT = FastCore.lookupFunction("fastdirectwrite", "fastdw_measure_text",
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_FLOAT,
                            ValueLayout.JAVA_FLOAT,
                            ValueLayout.ADDRESS
                    ),
                    FastDirectWrite.class, arena);

            MH_RENDER_TEXT = FastCore.lookupFunction("fastdirectwrite", "fastdw_render_text",
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS
                    ),
                    FastDirectWrite.class, arena);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    private final MemorySegment ctx;

    private FastDirectWrite(MemorySegment ctx) {
        this.ctx = ctx;
    }

    public static FastDirectWrite create() {
        try {
            MemorySegment ptr = (MemorySegment) MH_CREATE.invokeExact();
            if (ptr == null || ptr.equals(MemorySegment.NULL)) {
                throw new IllegalStateException("Failed to initialize DirectWrite context via fastdirectwrite.dll");
            }
            return new FastDirectWrite(ptr);
        } catch (Throwable t) {
            throw new RuntimeException("Error initializing FastDirectWrite", t);
        }
    }

    public DirectWriteFont font(String familyName, float size, FontWeight weight, FontStyle style) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment familySeg = arena.allocateFrom(familyName != null ? familyName : "Segoe UI Variable Text", StandardCharsets.UTF_16LE);
            MemorySegment localeSeg = arena.allocateFrom("en-us", StandardCharsets.UTF_16LE);

            MemorySegment fontHandle = (MemorySegment) MH_CREATE_FONT.invokeExact(
                    ctx,
                    familySeg,
                    size,
                    weight != null ? weight.value() : FontWeight.REGULAR.value(),
                    style != null ? style.value() : FontStyle.NORMAL.value(),
                    5, // DWRITE_FONT_STRETCH_NORMAL
                    localeSeg
            );

            if (fontHandle == null || fontHandle.equals(MemorySegment.NULL)) {
                throw new IllegalArgumentException("Failed to create DirectWrite font for: " + familyName);
            }

            return new DirectWriteFont(fontHandle, familyName, size, weight, style);
        } catch (Throwable t) {
            throw new RuntimeException("Error creating font", t);
        }
    }

    public DirectWriteFont font(String familyName, float size) {
        return font(familyName, size, FontWeight.REGULAR, FontStyle.NORMAL);
    }

    public TextMetrics measureText(String text, DirectWriteFont font) {
        return measureText(text, font, 100000.0f, 100000.0f);
    }

    public TextMetrics measureText(String text, DirectWriteFont font, float maxWidth, float maxHeight) {
        if (text == null || text.isEmpty()) {
            return new TextMetrics(0, 0, 0, 0, 0, 0);
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment textSeg = arena.allocateFrom(text, StandardCharsets.UTF_16LE);
            MemorySegment metricsSeg = arena.allocate(TextMetrics.LAYOUT);

            int res = (int) MH_MEASURE_TEXT.invokeExact(
                    ctx,
                    font.handle(),
                    textSeg,
                    text.length(),
                    maxWidth,
                    maxHeight,
                    metricsSeg
            );

            if (res == 0) {
                throw new IllegalStateException("DirectWrite failed to measure text: " + text);
            }

            return TextMetrics.fromSegment(metricsSeg);
        } catch (Throwable t) {
            throw new RuntimeException("Error measuring text", t);
        }
    }

    public DirectWriteBitmap renderText(String text, DirectWriteFont font, int colorArgb, Arena destinationArena) {
        TextMetrics metrics = measureText(text, font);
        int w = Math.max(1, (int) Math.ceil(metrics.widthIncludingTrailingWhitespace() > 0 ? metrics.widthIncludingTrailingWhitespace() : metrics.width()));
        int h = Math.max(1, (int) Math.ceil(metrics.height()));

        return renderText(text, font, colorArgb, w, h, destinationArena);
    }

    public DirectWriteBitmap renderText(String text, DirectWriteFont font, int colorArgb, int width, int height, Arena destinationArena) {
        try (Arena localArena = Arena.ofConfined()) {
            MemorySegment textSeg = localArena.allocateFrom(text, StandardCharsets.UTF_16LE);
            MemorySegment pixelBuffer = destinationArena.allocate((long) width * height * 4);

            int res = (int) MH_RENDER_TEXT.invokeExact(
                    ctx,
                    font.handle(),
                    textSeg,
                    text.length(),
                    colorArgb,
                    width,
                    height,
                    pixelBuffer
            );

            if (res == 0) {
                throw new IllegalStateException("DirectWrite failed to render text: " + text);
            }

            return new DirectWriteBitmap(width, height, pixelBuffer);
        } catch (Throwable t) {
            throw new RuntimeException("Error rendering text", t);
        }
    }

    static void destroyFont(DirectWriteFont font) {
        if (font != null && font.handle() != null && !font.handle().equals(MemorySegment.NULL)) {
            try {
                MH_DESTROY_FONT.invokeExact(font.handle());
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public void close() {
        if (ctx != null && !ctx.equals(MemorySegment.NULL)) {
            try {
                MH_DESTROY.invokeExact(ctx);
            } catch (Throwable ignored) {}
        }
    }
}
