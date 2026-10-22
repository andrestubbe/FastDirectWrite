#pragma once
#include <windows.h>
#include <dwrite.h>
#include <d2d1.h>
#include <cstdint>

#ifdef FASTDIRECTWRITE_EXPORTS
#define FDW_API extern "C" __declspec(dllexport)
#else
#define FDW_API extern "C" __declspec(dllimport)
#endif

// Opaque handles
typedef void* FDW_Context;
typedef void* FDW_Font;

#pragma pack(push, 1)
struct FDW_TextMetrics {
    float width;
    float height;
    float widthIncludingTrailingWhitespace;
    float layoutWidth;
    float layoutHeight;
    uint32_t lineCount;
};
#pragma pack(pop)

FDW_API FDW_Context fastdw_create();
FDW_API void fastdw_destroy(FDW_Context ctx);

FDW_API FDW_Font fastdw_create_font(
    FDW_Context ctx,
    const wchar_t* familyName,
    float fontSize,
    int fontWeight,
    int fontStyle,
    int fontStretch,
    const wchar_t* localeName
);
FDW_API void fastdw_destroy_font(FDW_Font font);

FDW_API int fastdw_measure_text(
    FDW_Context ctx,
    FDW_Font font,
    const wchar_t* text,
    int textLength,
    float maxWidth,
    float maxHeight,
    FDW_TextMetrics* outMetrics
);

// Renders text directly into a 32-bit BGRA/RGBA pixel buffer (stride = width * 4)
FDW_API int fastdw_render_text(
    FDW_Context ctx,
    FDW_Font font,
    const wchar_t* text,
    int textLength,
    uint32_t colorArgb,
    int width,
    int height,
    uint32_t* outPixels
);
