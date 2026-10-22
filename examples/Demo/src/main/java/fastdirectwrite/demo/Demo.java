package fastdirectwrite.demo;

import fastdirectwrite.DirectWriteBitmap;
import fastdirectwrite.DirectWriteFont;
import fastdirectwrite.FastDirectWrite;
import fastdirectwrite.FontStyle;
import fastdirectwrite.FontWeight;
import fastdirectwrite.TextMetrics;
import fastdirectx.DirectXBackend;
import fastgraphics.backend.BackendTexture;
import fastgraphics.g2d.FastGraphics2D;
import fasttheme.FastTheme;
import fastwindow.FastNativeWindow;
import fastwindow.FastWindow;

import java.lang.foreign.Arena;

/**
 * <h1>FastDirectWrite Interactive Showcase Demo</h1>
 * Renders native Windows 11 Segoe UI Variable fonts with DirectWrite ClearType anti-aliasing.
 * 100% Zero-AWT: No java.desktop, no java.awt.Font, no java.awt.image.BufferedImage!
 */
public class Demo {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  FastDirectWrite — Native Windows 11 Typography ");
        System.out.println("  Zero-AWT, DirectWrite Subpixel Anti-Aliasing    ");
        System.out.println("=================================================");

        final int width = 860;
        final int height = 540;

        // 1. Create native Win32 window with Mica dark mode
        try (FastNativeWindow window = FastWindow.create("FastDirectWrite — Native Typography", width, height);
             DirectXBackend dxBackend = new DirectXBackend();
             FastDirectWrite dw = FastDirectWrite.create()) {

            long hwnd = window.getHWND();
            if (hwnd != 0) {
                FastTheme.setTitleBarDarkMode(hwnd, true);
                FastTheme.setTitleBarColor(hwnd, 20, 20, 20);
                FastTheme.setTitleBarTextColor(hwnd, 240, 240, 240);
                FastTheme.setWindowBackgroundColor(hwnd, 18, 18, 20);
                FastTheme.setCornerStyle(hwnd, 2);
            }

            // 2. Initialize DirectX 11 GPU backend & FastGraphics2D
            dxBackend.initialize(hwnd, width, height);
            FastGraphics2D g2d = new FastGraphics2D(dxBackend, width, height);

            // 3. Create native Windows 11 Fonts (Segoe UI Variable & Cascadia Code)
            DirectWriteFont headerFont = dw.font("Segoe UI Variable Display", 32, FontWeight.BOLD, FontStyle.NORMAL);
            DirectWriteFont subFont    = dw.font("Segoe UI Variable Text", 16, FontWeight.REGULAR, FontStyle.NORMAL);
            DirectWriteFont badgeFont  = dw.font("Segoe UI Variable Small", 12, FontWeight.SEMI_BOLD, FontStyle.NORMAL);
            DirectWriteFont codeFont   = dw.font("Cascadia Code", 13, FontWeight.REGULAR, FontStyle.NORMAL);

            // Measure text via DirectWrite (Zero AWT FontMetrics!)
            TextMetrics tmTitle = dw.measureText("FastDirectWrite", headerFont);
            System.out.printf("[DirectWrite] Measured 'FastDirectWrite': width=%.2f px, height=%.2f px%n",
                    tmTitle.width(), tmTitle.height());

            // 4. Render strings directly into GPU textures using Arena
            Arena renderArena = Arena.ofShared();

            BackendTexture titleTex = uploadTextTexture(dxBackend, dw.renderText("Next-Gen Fluent Typography", headerFont, 0xFFFFFFFF, renderArena));
            BackendTexture subTex   = uploadTextTexture(dxBackend, dw.renderText("Gestochen scharfer Subpixel-Text via dwrite.dll — 100% ohne java.desktop / AWT!", subFont, 0xFFCCCCCC, renderArena));
            BackendTexture badge1   = uploadTextTexture(dxBackend, dw.renderText("0.00 % CPU Idle", badgeFont, 0xFF00E676, renderArena));
            BackendTexture badge2   = uploadTextTexture(dxBackend, dw.renderText("DirectWrite Native", badgeFont, 0xFF00B0FF, renderArena));
            BackendTexture badge3   = uploadTextTexture(dxBackend, dw.renderText("Zero AWT / No FreeType", badgeFont, 0xFFFFAB00, renderArena));
            BackendTexture codeTex  = uploadTextTexture(dxBackend, dw.renderText("FastDirectWrite.measureText() -> Exact DirectWrite text run layout", codeFont, 0xFFE0E0E0, renderArena));

            // 5. Ultra-efficient render callback
            fastwindow.WindowPaintListener renderFrame = (curW, curH) -> {
                dxBackend.beginFrame();
                dxBackend.clear(0.07f, 0.07f, 0.08f, 1.0f);

                g2d.begin();

                // Render Header
                g2d.drawImage(titleTex, 40, 45, titleTex.getWidth(), titleTex.getHeight());
                g2d.drawImage(subTex, 40, 95, subTex.getWidth(), subTex.getHeight());

                // Render Badge backgrounds (Fluent Accent Cards)
                drawCard(g2d, 40, 140, badge1.getWidth() + 24, badge1.getHeight() + 12, 0x2200E676);
                g2d.drawImage(badge1, 52, 146, badge1.getWidth(), badge1.getHeight());

                drawCard(g2d, 40 + badge1.getWidth() + 36, 140, badge2.getWidth() + 24, badge2.getHeight() + 12, 0x2200B0FF);
                g2d.drawImage(badge2, 40 + badge1.getWidth() + 48, 146, badge2.getWidth(), badge2.getHeight());

                drawCard(g2d, 40 + badge1.getWidth() + badge2.getWidth() + 72, 140, badge3.getWidth() + 24, badge3.getHeight() + 12, 0x22FFAB00);
                g2d.drawImage(badge3, 40 + badge1.getWidth() + badge2.getWidth() + 84, 146, badge3.getWidth(), badge3.getHeight());

                // Render Code Box
                drawCard(g2d, 40, 200, 780, 80, 0x1AFFFFFF);
                g2d.drawImage(codeTex, 60, 228, codeTex.getWidth(), codeTex.getHeight());

                g2d.end();
                dxBackend.endFrame();
                dxBackend.present();
            };

            window.setPaintListener(renderFrame);

            // Zero-CPU Event Loop via waitEvents()
            while (window.waitEvents()) {
                renderFrame.onPaint(width, height);
            }

            // Cleanup
            dxBackend.destroyTexture(titleTex);
            dxBackend.destroyTexture(subTex);
            dxBackend.destroyTexture(badge1);
            dxBackend.destroyTexture(badge2);
            dxBackend.destroyTexture(badge3);
            dxBackend.destroyTexture(codeTex);
            renderArena.close();
            headerFont.close();
            subFont.close();
            badgeFont.close();
            codeFont.close();
        }
    }

    private static BackendTexture uploadTextTexture(DirectXBackend backend, DirectWriteBitmap bitmap) {
        return backend.createTexture(
                bitmap.width(),
                bitmap.height(),
                bitmap.pixels().asByteBuffer()
        );
    }

    private static void drawCard(FastGraphics2D g, float x, float y, float w, float h, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float gC = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;
        float a = ((argb >> 24) & 0xFF) / 255.0f;
        g.setColor(r, gC, b, a);
        g.fillRect(x, y, w, h);
    }
}
