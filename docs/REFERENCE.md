# FastDirectWrite API Reference Manual

Welcome to the **FastDirectWrite** technical reference. FastDirectWrite provides a high-performance, zero-AWT Microsoft DirectWrite and Direct2D typography engine for Java via Foreign Function & Memory (FFM) downcalls.

---

## 1. Core Architecture

FastDirectWrite establishes a direct bridge between the JVM and Windows 10/11 DirectWrite typography subsystem:

```
┌────────────────────────────────────────────────────────┐
│                      FastDirectWrite                   │
│   DirectWriteFont  •  TextMetrics  •  DirectWriteBitmap│
└───────────────────────────┬────────────────────────────┘
                            │ FFM Downcalls (MemorySegment)
                            ▼
┌────────────────────────────────────────────────────────┐
│                   fastdirectwrite.dll                  │
│       IDWriteFactory  •  ID2D1Factory  •  ID2D1DC-RT   │
└───────────────────────────┬────────────────────────────┘
                            │ Direct Win32 COM ABI
                            ▼
┌────────────────────────────────────────────────────────┐
│           Microsoft Windows DirectWrite (dwrite.dll)   │
│         Direct2D Subpixel ClearType (d2d1.dll)         │
└────────────────────────────────────────────────────────┘
```

---

## 2. FastDirectWrite Class

The primary orchestrator and context factory.

### Factory & Lifecycle
- `static FastDirectWrite create()`: Initializes the native DirectWrite and Direct2D factories (`fastdw_create`).
- `void close()`: Releases all COM factories and native context structures (`fastdw_destroy`).

### Font Management
- `DirectWriteFont font(String familyName, float size)`: Creates a font instance with regular weight and normal style.
- `DirectWriteFont font(String familyName, float size, FontWeight weight, FontStyle style)`: Creates a font instance with custom weight and style.

### Typography & Measurement
- `TextMetrics measureText(String text, DirectWriteFont font)`: Performs layout and returns exact bounding metrics.
- `TextMetrics measureText(String text, DirectWriteFont font, float maxWidth, float maxHeight)`: Performs constrained multi-line layout measurement.

### Rendering
- `DirectWriteBitmap renderText(String text, DirectWriteFont font, int colorArgb, Arena destinationArena)`: Renders text to an auto-sized bitmap buffer allocated in the given `Arena`.
- `DirectWriteBitmap renderText(String text, DirectWriteFont font, int colorArgb, int width, int height, Arena destinationArena)`: Renders text to a fixed-size bitmap buffer.

---

## 3. Data Structures

### `DirectWriteFont`
Represents an instantiated native `IDWriteTextFormat` handle.
- Implements `AutoCloseable`.
- Methods: `handle()`, `familyName()`, `size()`, `weight()`, `style()`, `close()`.

### `TextMetrics`
Immutable layout calculation results from `IDWriteTextLayout::GetMetrics`.
- `width()`: Float width of formatted text (excluding trailing whitespace).
- `height()`: Float height of formatted text.
- `widthIncludingTrailingWhitespace()`: Total width including spaces.
- `layoutWidth()`: Initial width constraint used for formatting.
- `layoutHeight()`: Initial height constraint used for formatting.
- `lineCount()`: Total number of formatted lines.

### `DirectWriteBitmap`
Represents rasterized 32-bit BGRA/RGBA pixels in native memory.
- `width()`: Width in pixels.
- `height()`: Height in pixels.
- `pixels()`: Off-heap `MemorySegment` containing 32-bit pixel data.

---

## 4. Typography Enums

### `FontWeight`
- `THIN (100)`
- `EXTRA_LIGHT (200)`
- `LIGHT (300)`
- `SEMI_LIGHT (350)`
- `REGULAR (400)`
- `MEDIUM (500)`
- `SEMI_BOLD (600)`
- `BOLD (700)`
- `EXTRA_BOLD (800)`
- `BLACK (900)`
- `EXTRA_BLACK (950)`

### `FontStyle`
- `NORMAL (0)`
- `OBLIQUE (1)`
- `ITALIC (2)`
