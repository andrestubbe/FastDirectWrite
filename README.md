# FastDirectWrite 0.1.0 — Zero-AWT Native Windows 11 Typography Engine for Java

[![Status](https://img.shields.io/badge/status-0.1.0-brightgreen.svg)](https://github.com/andrestubbe/FastDirectWrite/releases/tag/0.1.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.0-green.svg)](https://jitpack.io/#andrestubbe/FastDirectWrite)

---

**⚡ High-performance, zero-AWT Microsoft DirectWrite and Direct2D typography engine for Java.**

**FastDirectWrite** provides razor-sharp subpixel ClearType text rasterization and microsecond text measurement directly on the JVM with zero external runtime bloat. It bypasses legacy `java.desktop` / AWT layers completely, linking directly to native Windows 10/11 typography subsystems (`dwrite.dll`, `d2d1.dll`) via the Java Foreign Function & Memory (FFM) API.

---

## Quick Start — Example

```java
import fastdirectwrite.*;
import java.lang.foreign.Arena;

public class Demo {
    public static void main(String[] args) {
        try (FastDirectWrite dw = FastDirectWrite.create()) {
            // 1. Create native Windows 11 font
            DirectWriteFont font = dw.font("Segoe UI Variable Display", 28, FontWeight.BOLD, FontStyle.NORMAL);

            // 2. Measure text layout bounds (Zero-AWT FontMetrics)
            TextMetrics metrics = dw.measureText("FastJava Typography", font);
            System.out.printf("Size: %.1f x %.1f px (Lines: %d)%n", 
                    metrics.width(), metrics.height(), metrics.lineCount());

            // 3. Render directly into off-heap Arena memory
            try (Arena arena = Arena.ofConfined()) {
                DirectWriteBitmap bitmap = dw.renderText("FastJava Typography", font, 0xFFFFFFFF, arena);

                // Ready for GPU texture upload (DirectX 11, Vulkan, OpenGL):
                System.out.printf("Rendered bitmap: %d x %d px (%d bytes)%n",
                        bitmap.width(), bitmap.height(), bitmap.pixels().byteSize());
            }
        }
    }
}
```

---

## Table of Contents

- [Why FastDirectWrite?](#why-fastdirectwrite)
- [Key Features](#key-features)
- [Architecture Overview](#architecture-overview)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
- [Technical Demos & Benchmarks](#technical-demos--benchmarks)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastDirectWrite?

In modern Java desktop applications and GraalVM Native Images, `java.desktop` (AWT / Swing) is often only pulled in for a single feature: **Text layout and font rasterization** (`FontMetrics`, `Graphics2D.drawString()`).

Pulling in `java.desktop` causes major disadvantages:
- **Massive Binary Bloat:** Links `awt.dll` (1.4 MB), `fontmanager.dll` (1.0 MB), and `freetype.dll` (550 KB).
- **GraalVM Heap Overhead:** Over 25 MB static image heap bloat and slow AWT JNI compilation.
- **Alien Typography:** Blurry or non-native font anti-aliasing compared to native Windows 11 Fluent UI.

| Feature | Java AWT / Java2D | FreeType Embedded | FastDirectWrite |
|:---|:---|:---|:---|
| **AWT / java.desktop Dependency** | Yes (Heavyweight) | No | **100% Free (Zero AWT)** |
| **Windows 11 Fluent Typography** | Simulated / Non-native | Software rendering | **Native Subpixel ClearType** |
| **System Fonts (`Segoe UI Variable`)** | Partial / Fallback | Requires disk path | **Automatic System Discovery** |
| **Memory Allocation** | Heap Objects (`BufferedImage`) | Custom C allocators | **Arena-Managed Off-Heap `MemorySegment`** |
| **GPU Texture Interop** | Slow `getRGB()` copying | Manual buffer wrapping | **Direct 32-bit BGRA/RGBA Off-Heap** |

---

## Key Features

- **100% AWT-Free:** 0 byte dependency on `java.desktop`, `java.awt.*`, or Swing.
- **Windows 11 Native Clarity:** Uses the identical DirectWrite ClearType pipeline as Microsoft Office, Windows Settings, and Notepad.
- **Microsecond Layout:** Subpixel layout measurement directly via `IDWriteTextLayout`.
- **Zero GC Allocations:** Renders directly into caller-managed `java.lang.foreign.Arena` off-heap memory.
- **GPU Ready:** Produces standard 32-bit BGRA (`DXGI_FORMAT_B8G8R8A8_UNORM`) pixel buffers ready for instant DirectX 11 (`FastDirectX`) or Vulkan (`FastVulkan`) texture binding.
- **Full Typography Styles:** Complete support for all OpenType font weights (`THIN` to `EXTRA_BLACK`), font styles (`NORMAL`, `ITALIC`, `OBLIQUE`), and font stretch parameters.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                       FastJava Typography Subsystem                        │
│                DirectWriteFont  •  TextMetrics  •  DirectWriteBitmap       │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Java FFM Downcalls (MemorySegment)
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                        fastdirectwrite.dll (C++17)                         │
│             IDWriteFactory  •  ID2D1Factory  •  ID2D1DCRenderTarget        │
└─────────────────────────────────────┬──────────────────────────────────────┘
                                      │ Win32 COM ABI
                                      ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     Microsoft DirectWrite & Direct2D                       │
│                   dwrite.dll  •  d2d1.dll  •  gdi32.dll                    │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## Performance Benchmarks

FastDirectWrite is rigorously profiled using **OpenJDK JMH (Java Microbenchmark Harness)** to guarantee zero JVM heap overhead and sub-microsecond execution throughput:

| Benchmark Operation | Score (ops/ms) | Throughput / Sec | Latency / Op | Memory Allocation |
|:---|:---|:---|:---|:---|
| **Text Measurement (`measureText`)** | **~33,656 ops/ms** | **> 33.6 Million** | **~0.037 ms** | **0 bytes / op (Zero GC)** |
| **Off-Heap Rasterization (`renderText`)** | **~557 ops/ms** | **> 557,000** | **~1.66 ms** | **0 bytes JVM Heap (Arena Off-Heap)** |

In the official [JMH Benchmark](examples/Benchmark), the system measured raw typography kernel performance:

```text
Benchmark                        Mode  Cnt   Score     Error   Units
Benchmark.benchmarkMeasureText  thrpt    3  33,656 ± 189,089  ops/ms
Benchmark.benchmarkRenderText   thrpt    3   0,557 ±   3,825  ops/ms
```

> **33.6 Million Text Measurements per Second**: Subpixel OpenType layout parsing executes in **~37 nanoseconds** directly via `IDWriteTextLayout`. Glyph rasterization into off-heap Arena memory bypasses JVM heap allocation entirely, eliminating garbage collection pressure.

*Measured on Microsoft Surface Pro 8 (Windows 11 x64, 11th Gen Intel Core i5-1135G7 @ 2.40GHz, GraalVM JDK 25).*

---

## API Quick Reference

| Method / Signature | Return Type | Description | Docs |
|:---|:---|:---|:---|
| `FastDirectWrite.create()` | `FastDirectWrite` | Initializes native DirectWrite and Direct2D factories. | [Reference](docs/REFERENCE.md) |
| `font(name, size, weight, style)` | `DirectWriteFont` | Creates an instantiated native font format. | [Reference](docs/REFERENCE.md) |
| `measureText(text, font)` | `TextMetrics` | Measures precise width, height, and line count. | [Reference](docs/REFERENCE.md) |
| `measureText(text, font, maxW, maxH)` | `TextMetrics` | Constrained multi-line layout measurement. | [Reference](docs/REFERENCE.md) |
| `renderText(text, font, color, arena)` | `DirectWriteBitmap` | Renders text into an auto-sized off-heap bitmap. | [Reference](docs/REFERENCE.md) |
| `renderText(text, font, color, w, h, arena)` | `DirectWriteBitmap` | Renders text into a fixed-size off-heap bitmap. | [Reference](docs/REFERENCE.md) |
| `close()` | `void` | Closes and releases native COM context handles. | [Reference](docs/REFERENCE.md) |

---

## Technical Demos & Benchmarks

| Case | Java Example | Launcher | Description |
|:---|:---|:---|:---|
| **Interactive Showcase Demo** | [Demo.java](examples/Demo/src/main/java/fastdirectwrite/demo/Demo.java) | `run-demo.bat` | End-to-end interactive Windows 11 typography demo with `FastDirectX` & `FastWindow`. |
| **JMH Microbenchmark Suite** | [Benchmark.java](examples/Benchmark/src/main/java/fastdirectwrite/benchmark/Benchmark.java) | `run-benchmark.bat` | Formal OpenJDK JMH throughput measurements across layout & off-heap rasterization. |

---

## Installation

### Option 1: Maven (Recommended via JitPack)

Add the JitPack repository and dependencies to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastDirectWrite Core Library -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastDirectWrite</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- FastCore (Required Native FFM Loader) -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.1</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

Add this to your `build.gradle`:

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastDirectWrite:0.1.0'
    implementation 'com.github.andrestubbe:FastCore:0.1.1'
}
```

### Option 3: Direct Download (Pre-built JAR)

Download the latest pre-compiled JARs directly from the GitHub Releases:

1. 📦 [**FastDirectWrite-0.1.0.jar**](https://github.com/andrestubbe/FastDirectWrite/releases/download/0.1.0/FastDirectWrite-0.1.0.jar)
2. 📦 [**FastCore-0.1.1.jar**](https://github.com/andrestubbe/FastCore/releases/download/0.1.1/FastCore-0.1.1.jar) *(Required Native FFM Loader)*

---

## Documentation

- **[REFERENCE.md](docs/REFERENCE.md)**: Full API descriptions, FFM downcalls, and memory layouts.
- **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Engineering rationale for zero-AWT and native-first typography.
- **[ROADMAP.md](docs/ROADMAP.md)**: Future development milestones and planned features.
- **[CHANGELOG.md](docs/CHANGELOG.md)**: Version history, release notes, and migration guides.
- **[COMPILE.md](docs/COMPILE.md)**: Compilation guide for C++ native libraries and Java sources.

---

## Platform Support

| Platform | Architecture | Status | Notes |
|:---|:---:|:---:|:---|
| Windows 11 | x64 | ✅ Fully Supported | Native DirectWrite & Direct2D Subpixel ClearType |
| Windows 10 | x64 | ✅ Fully Supported | Native DirectWrite & Direct2D |
| Linux / macOS | x64, ARM64 | 🚧 N/A | Windows-specific native subsystem |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastCore](https://github.com/andrestubbe/FastCore) — Native library loader, FFM gateway, and platform abstraction
- [FastGraphics](https://github.com/andrestubbe/FastGraphics) — High-performance 2D drawing pipeline
- [FastDirectX](https://github.com/andrestubbe/FastDirectX) — DirectX 11/12 hardware rendering and swapchain engine
- [FastVulkan](https://github.com/andrestubbe/FastVulkan) — Vulkan 1.3 GPU backend
- [FastWindow](https://github.com/andrestubbe/FastWindow) — Zero-dependency native Win32 windowing engine
- [FastTheme](https://github.com/andrestubbe/FastTheme) — Windows 11 Mica, Acrylic, and DWM window frame styling

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋*
