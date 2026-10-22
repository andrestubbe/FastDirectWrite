# FastDirectWrite Compilation Guide

FastDirectWrite consists of a lightweight C++17 native dynamic link library (`fastdirectwrite.dll`) and a Java 25 Foreign Function & Memory (FFM) library.

---

## 1. Prerequisites

- **Windows 10 / 11 (64-bit)**
- **Microsoft Visual Studio 2022+** (Community or Build Tools) with *Desktop development with C++*
- **GraalVM JDK 25+** or **Oracle JDK 25+** (Java 22+ with Foreign Function & Memory API)
- **Apache Maven 3.9+**

---

## 2. Compiling the Native Library

FastDirectWrite includes an automated batch script `compile.bat` that automatically discovers your Visual Studio MSVC environment:

```cmd
cd FastDirectWrite
.\compile.bat
```

The build script invokes MSVC `cl.exe`:
```cmd
cl.exe /O2 /LD /EHsc /std:c++17 /W3 /DUNICODE /D_UNICODE ^
    native\FastDirectWrite.cpp ^
    /link /DLL /OUT:fastdirectwrite.dll user32.lib gdi32.lib dwrite.lib d2d1.lib ole32.lib
```

It places the resulting `fastdirectwrite.dll` into:
1. `./fastdirectwrite.dll` (root)
2. `src/main/resources/native/fastdirectwrite.dll` (packaged inside JAR)
3. `target/classes/native/fastdirectwrite.dll` (immediate test runtime)

---

## 3. Compiling the Java Library

Build the JAR using Maven:

```cmd
mvn clean package -DskipTests
```

---

## 4. Running the Interactive Demo

Start the hardware-accelerated Windows 11 typography demo:

```cmd
.\run-demo.bat
```
