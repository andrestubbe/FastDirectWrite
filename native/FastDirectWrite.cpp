#define FASTDIRECTWRITE_EXPORTS
#include "FastDirectWrite.h"
#include <wrl/client.h>

#pragma comment(lib, "dwrite.lib")
#pragma comment(lib, "d2d1.lib")

using Microsoft::WRL::ComPtr;

struct FDW_ContextImpl {
    ComPtr<IDWriteFactory> dwriteFactory;
    ComPtr<ID2D1Factory> d2dFactory;
};

struct FDW_FontImpl {
    ComPtr<IDWriteTextFormat> textFormat;
    float fontSize;
};

FDW_API FDW_Context fastdw_create() {
    FDW_ContextImpl* ctx = new FDW_ContextImpl();
    
    HRESULT hr = DWriteCreateFactory(
        DWRITE_FACTORY_TYPE_SHARED,
        __uuidof(IDWriteFactory),
        reinterpret_cast<IUnknown**>(ctx->dwriteFactory.GetAddressOf())
    );
    if (FAILED(hr)) {
        delete ctx;
        return nullptr;
    }

    hr = D2D1CreateFactory(
        D2D1_FACTORY_TYPE_SINGLE_THREADED,
        ctx->d2dFactory.GetAddressOf()
    );
    if (FAILED(hr)) {
        delete ctx;
        return nullptr;
    }

    return ctx;
}

FDW_API void fastdw_destroy(FDW_Context ctxHandle) {
    if (!ctxHandle) return;
    FDW_ContextImpl* ctx = static_cast<FDW_ContextImpl*>(ctxHandle);
    delete ctx;
}

FDW_API FDW_Font fastdw_create_font(
    FDW_Context ctxHandle,
    const wchar_t* familyName,
    float fontSize,
    int fontWeight,
    int fontStyle,
    int fontStretch,
    const wchar_t* localeName
) {
    if (!ctxHandle) return nullptr;
    FDW_ContextImpl* ctx = static_cast<FDW_ContextImpl*>(ctxHandle);

    FDW_FontImpl* font = new FDW_FontImpl();
    font->fontSize = fontSize;

    HRESULT hr = ctx->dwriteFactory->CreateTextFormat(
        familyName ? familyName : L"Segoe UI Variable Text",
        nullptr,
        static_cast<DWRITE_FONT_WEIGHT>(fontWeight > 0 ? fontWeight : DWRITE_FONT_WEIGHT_NORMAL),
        static_cast<DWRITE_FONT_STYLE>(fontStyle >= 0 ? fontStyle : DWRITE_FONT_STYLE_NORMAL),
        static_cast<DWRITE_FONT_STRETCH>(fontStretch > 0 ? fontStretch : DWRITE_FONT_STRETCH_NORMAL),
        fontSize > 0.0f ? fontSize : 14.0f,
        localeName ? localeName : L"en-us",
        font->textFormat.GetAddressOf()
    );

    if (FAILED(hr)) {
        delete font;
        return nullptr;
    }

    return font;
}

FDW_API void fastdw_destroy_font(FDW_Font fontHandle) {
    if (!fontHandle) return;
    FDW_FontImpl* font = static_cast<FDW_FontImpl*>(fontHandle);
    delete font;
}

FDW_API int fastdw_measure_text(
    FDW_Context ctxHandle,
    FDW_Font fontHandle,
    const wchar_t* text,
    int textLength,
    float maxWidth,
    float maxHeight,
    FDW_TextMetrics* outMetrics
) {
    if (!ctxHandle || !fontHandle || !text || !outMetrics) return 0;
    FDW_ContextImpl* ctx = static_cast<FDW_ContextImpl*>(ctxHandle);
    FDW_FontImpl* font = static_cast<FDW_FontImpl*>(fontHandle);

    uint32_t len = (textLength >= 0) ? static_cast<uint32_t>(textLength) : static_cast<uint32_t>(wcslen(text));
    if (len == 0) {
        memset(outMetrics, 0, sizeof(FDW_TextMetrics));
        return 1;
    }

    float layoutW = (maxWidth > 0.0f) ? maxWidth : 100000.0f;
    float layoutH = (maxHeight > 0.0f) ? maxHeight : 100000.0f;

    ComPtr<IDWriteTextLayout> layout;
    HRESULT hr = ctx->dwriteFactory->CreateTextLayout(
        text,
        len,
        font->textFormat.Get(),
        layoutW,
        layoutH,
        layout.GetAddressOf()
    );
    if (FAILED(hr)) return 0;

    DWRITE_TEXT_METRICS tm;
    hr = layout->GetMetrics(&tm);
    if (FAILED(hr)) return 0;

    outMetrics->width = tm.width;
    outMetrics->height = tm.height;
    outMetrics->widthIncludingTrailingWhitespace = tm.widthIncludingTrailingWhitespace;
    outMetrics->layoutWidth = tm.layoutWidth;
    outMetrics->layoutHeight = tm.layoutHeight;
    outMetrics->lineCount = tm.lineCount;

    return 1;
}

FDW_API int fastdw_render_text(
    FDW_Context ctxHandle,
    FDW_Font fontHandle,
    const wchar_t* text,
    int textLength,
    uint32_t colorArgb,
    int width,
    int height,
    uint32_t* outPixels
) {
    if (!ctxHandle || !fontHandle || !text || !outPixels || width <= 0 || height <= 0) return 0;
    FDW_ContextImpl* ctx = static_cast<FDW_ContextImpl*>(ctxHandle);
    FDW_FontImpl* font = static_cast<FDW_FontImpl*>(fontHandle);

    uint32_t len = (textLength >= 0) ? static_cast<uint32_t>(textLength) : static_cast<uint32_t>(wcslen(text));
    if (len == 0) return 1;

    // Clear destination buffer to 0 (transparent)
    memset(outPixels, 0, width * height * sizeof(uint32_t));

    // Create a D2D WIC/DIB render target targeting the system memory pixel buffer
    D2D1_RENDER_TARGET_PROPERTIES props = D2D1::RenderTargetProperties(
        D2D1_RENDER_TARGET_TYPE_DEFAULT,
        D2D1::PixelFormat(DXGI_FORMAT_B8G8R8A8_UNORM, D2D1_ALPHA_MODE_PREMULTIPLIED),
        0, 0,
        D2D1_RENDER_TARGET_USAGE_NONE,
        D2D1_FEATURE_LEVEL_DEFAULT
    );

    // Create WIC imaging or D2D memory bitmap render target
    ComPtr<ID2D1DCRenderTarget> dcRt;
    HRESULT hr = ctx->d2dFactory->CreateDCRenderTarget(&props, dcRt.GetAddressOf());
    if (FAILED(hr)) return 0;

    // Create a 32-bit DIB Section to render into
    BITMAPINFO bmi = {};
    bmi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bmi.bmiHeader.biWidth = width;
    bmi.bmiHeader.biHeight = -height; // Top-down
    bmi.bmiHeader.biPlanes = 1;
    bmi.bmiHeader.biBitCount = 32;
    bmi.bmiHeader.biCompression = BI_RGB;

    void* dibBits = nullptr;
    HDC hdcMem = CreateCompatibleDC(nullptr);
    HBITMAP hbm = CreateDIBSection(hdcMem, &bmi, DIB_RGB_COLORS, &dibBits, nullptr, 0);
    if (!hbm || !dibBits) {
        if (hdcMem) DeleteDC(hdcMem);
        return 0;
    }
    HGDIOBJ oldBm = SelectObject(hdcMem, hbm);

    RECT rc = { 0, 0, width, height };
    hr = dcRt->BindDC(hdcMem, &rc);
    if (SUCCEEDED(hr)) {
        dcRt->SetTextAntialiasMode(D2D1_TEXT_ANTIALIAS_MODE_CLEARTYPE);

        float a = ((colorArgb >> 24) & 0xFF) / 255.0f;
        float r = ((colorArgb >> 16) & 0xFF) / 255.0f;
        float g = ((colorArgb >> 8) & 0xFF) / 255.0f;
        float b = (colorArgb & 0xFF) / 255.0f;

        ComPtr<ID2D1SolidColorBrush> brush;
        dcRt->CreateSolidColorBrush(D2D1::ColorF(r, g, b, a), brush.GetAddressOf());

        dcRt->BeginDraw();
        dcRt->Clear(D2D1::ColorF(0, 0, 0, 0.0f));

        D2D1_RECT_F layoutRect = D2D1::RectF(0.0f, 0.0f, static_cast<float>(width), static_cast<float>(height));
        dcRt->DrawText(
            text,
            len,
            font->textFormat.Get(),
            &layoutRect,
            brush.Get(),
            D2D1_DRAW_TEXT_OPTIONS_ENABLE_COLOR_FONT
        );

        dcRt->EndDraw();

        // Copy DIB Section bits directly to outPixels
        memcpy(outPixels, dibBits, width * height * sizeof(uint32_t));
    }

    SelectObject(hdcMem, oldBm);
    DeleteObject(hbm);
    DeleteDC(hdcMem);

    return SUCCEEDED(hr) ? 1 : 0;
}
