package fastdirectwrite.benchmark;

import fastdirectwrite.DirectWriteBitmap;
import fastdirectwrite.DirectWriteFont;
import fastdirectwrite.FastDirectWrite;
import fastdirectwrite.FontStyle;
import fastdirectwrite.FontWeight;
import fastdirectwrite.TextMetrics;
import org.openjdk.jmh.annotations.*;

import java.lang.foreign.Arena;
import java.util.concurrent.TimeUnit;

/**
 * Standard OpenJDK JMH Microbenchmark Suite for FastDirectWrite.
 * Measures throughput and latency of font layout measurement and off-heap rasterization.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class Benchmark {

    private FastDirectWrite dw;
    private DirectWriteFont font;
    private Arena arena;

    @Setup
    public void setup() {
        dw = FastDirectWrite.create();
        font = dw.font("Segoe UI Variable Text", 16.0f, FontWeight.REGULAR, FontStyle.NORMAL);
        arena = Arena.ofShared();
    }

    @TearDown
    public void tearDown() {
        if (font != null) font.close();
        if (arena != null) arena.close();
        if (dw != null) dw.close();
    }

    @org.openjdk.jmh.annotations.Benchmark
    public TextMetrics benchmarkMeasureText() {
        return dw.measureText("FastJava Typography: Native Subpixel ClearType", font);
    }

    @org.openjdk.jmh.annotations.Benchmark
    public DirectWriteBitmap benchmarkRenderText() {
        return dw.renderText("FastDirectWrite Performance", font, 0xFFFFFFFF, arena);
    }
}
