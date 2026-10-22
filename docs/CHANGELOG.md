# Changelog

All notable changes to `FastDirectWrite` will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-10-22

### Added
- **Native DirectWrite & Direct2D Engine**: High-performance Win32 typography pipeline powered by `dwrite.dll` and `d2d1.dll` via MSVC C++17 native shim (`fastdirectwrite.dll`).
- **Zero-AWT Architecture**: 100% free of `java.desktop`, `java.awt.Font`, and `BufferedImage` dependencies.
- **Microsecond Font Layout & Metrics**: Precise subpixel text layout measurement via `IDWriteTextLayout` (`FastDirectWrite.measureText`).
- **Direct Memory Pixel Rendering**: Direct rendering to 32-bit BGRA/RGBA off-heap `MemorySegment` buffers with subpixel ClearType anti-aliasing via `ID2D1DCRenderTarget`.
- **Java FFM API Integration**: Zero JNI overhead downcalls using Java Foreign Function & Memory API (`java.lang.foreign.Arena`, `Linker`, `MethodHandle`).
- **Fluent UI Typography**: Seamless support for Windows 11 system fonts including `Segoe UI Variable Display`, `Segoe UI Variable Text`, `Segoe UI Variable Small`, and `Cascadia Code`.
- **Interactive Hardware Demo**: Hardware-accelerated demo using `FastWindow` (Mica/Dark mode) and `FastDirectX` (`DirectWriteDemoMain`).
