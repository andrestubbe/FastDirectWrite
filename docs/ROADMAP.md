# FastDirectWrite Roadmap

Development goals and milestones for `FastDirectWrite`:

---

## Short-Term Milestones (v0.1.x)
- [x] Initial Win32 DirectWrite (`dwrite.dll`) & Direct2D (`d2d1.dll`) C++ bridge.
- [x] Java FFM integration for zero-JNI native calls (`java.lang.foreign`).
- [x] Font creation with family name, size, weight, and style.
- [x] Subpixel text measurement (`measureText`) via `IDWriteTextLayout`.
- [x] Text rasterization to off-heap `MemorySegment` pixel buffers with ClearType.
- [x] Full integration demo with `FastDirectX` and `FastWindow`.

---

## Medium-Term Milestones (v0.2.x)
- [ ] **Glyph Run Extraction**: Direct extraction of glyph indices and curve outlines for GPU bezier vector rendering.
- [ ] **Custom In-Memory Font Loading**: Support loading OTF/TTF fonts directly from `byte[]` / `InputStream` into private font collections via `IDWriteFontFileLoader`.
- [ ] **Color Emoji Support**: Rasterization and layout of multi-color OpenType-SVG and COLR/CPAL emojis (`Segoe UI Emoji`).
- [ ] **Complex Script Bidi**: Full bidirectional (BiDi) right-to-left layout analysis and text wrapping.

---

## Long-Term Vision (v1.0.0)
- Unified typography provider for **FastGraphics** and **FastUI**, eliminating all legacy Java AWT dependencies across the entire FastJava desktop stack.
