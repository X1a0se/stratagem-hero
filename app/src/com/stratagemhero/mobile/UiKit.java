package com.stratagemhero.mobile;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

/** Shared visual language: ink-black panels, cream type and HELLDIVERS gold. */
final class UiKit {
    static final int BG = 0xFF030607;
    static final int BG_2 = 0xFF0A1013;
    static final int PANEL = 0xF20A1012;
    static final int PANEL_LIGHT = 0xFF172225;
    static final int CREAM = 0xFFF1EEDC;
    static final int MUTED = 0xFF9AA69D;
    static final int DIM = 0xFF66736C;
    static final int GOLD = 0xFFFFD24A;
    static final int GOLD_SOFT = 0xFFFFE7A1;
    static final int GOLD_DARK = 0xFF8C7021;
    static final int GREEN = 0xFF9BE58B;
    static final int BLUE = 0xFF75D6FF;
    static final int CORAL = 0xFFFF8E7A;
    static final int RED = 0xFFFF5D52;
    static final int LINE = 0x33F1EEDC;

    static final Typeface DISPLAY = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    static final Typeface BODY = Typeface.create("sans-serif", Typeface.NORMAL);
    static final Typeface BODY_BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    static final Typeface MONO = Typeface.create("monospace", Typeface.BOLD);

    private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path PATH = new Path();
    private static final RectF RECT = new RectF();

    private UiKit() {}

    static int categoryColor(String category) {
        if (StratagemCatalog.MISSION.equals(category)) return GOLD;
        if (StratagemCatalog.SUPPORT.equals(category)) return BLUE;
        if (StratagemCatalog.OFFENSIVE.equals(category)) return CORAL;
        return GREEN;
    }

    static String categoryZh(String category) {
        if (StratagemCatalog.MISSION.equals(category)) return "任务配备";
        if (StratagemCatalog.SUPPORT.equals(category)) return "支援武器";
        if (StratagemCatalog.OFFENSIVE.equals(category)) return "进攻配备";
        if (StratagemCatalog.BACKPACK.equals(category)) return "背包";
        if (StratagemCatalog.SENTRY.equals(category)) return "哨戒炮";
        return "防御阵地";
    }

    static Path chamfer(float left, float top, float right, float bottom, float cut) {
        PATH.reset();
        PATH.moveTo(left + cut, top);
        PATH.lineTo(right - cut, top);
        PATH.lineTo(right, top + cut);
        PATH.lineTo(right, bottom - cut);
        PATH.lineTo(right - cut, bottom);
        PATH.lineTo(left + cut, bottom);
        PATH.lineTo(left, bottom - cut);
        PATH.lineTo(left, top + cut);
        PATH.close();
        return PATH;
    }

    static void panel(Canvas c, float l, float t, float r, float b, float cut, int fill, int stroke) {
        Path p = chamfer(l, t, r, b, cut);
        PAINT.reset();
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(fill);
        c.drawPath(p, PAINT);
        if (stroke != 0) {
            PAINT.setStyle(Paint.Style.STROKE);
            PAINT.setStrokeWidth(1.4f);
            PAINT.setColor(stroke);
            c.drawPath(p, PAINT);
        }
    }

    static void line(Canvas c, float x1, float y1, float x2, float y2, float width, int color) {
        PAINT.reset();
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(width);
        PAINT.setStrokeCap(Paint.Cap.SQUARE);
        PAINT.setColor(color);
        c.drawLine(x1, y1, x2, y2, PAINT);
    }

    static void text(Canvas c, String s, float x, float y, float size, int color,
                     Paint.Align align, Typeface face) {
        if (s == null) return;
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setTypeface(face);
        PAINT.setTextSize(size);
        PAINT.setTextAlign(align);
        PAINT.setColor(color);
        c.drawText(s, x, y, PAINT);
    }

    static void textFit(Canvas c, String s, float x, float y, float maxSize, float minSize,
                        float maxWidth, int color, Paint.Align align, Typeface face) {
        if (s == null) return;
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setTypeface(face);
        PAINT.setTextAlign(align);
        float size = maxSize;
        PAINT.setTextSize(size);
        while (size > minSize && PAINT.measureText(s) > maxWidth) {
            size -= 0.5f;
            PAINT.setTextSize(size);
        }
        PAINT.setColor(color);
        c.drawText(s, x, y, PAINT);
    }

    static float measure(String s, float size, Typeface face) {
        if (s == null) return 0f;
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setTypeface(face);
        PAINT.setTextSize(size);
        return PAINT.measureText(s);
    }

    static void arrow(Canvas c, float cx, float cy, float size, Direction dir, int color) {
        if (dir == null) return;
        c.save();
        c.translate(cx, cy);
        switch (dir) {
            case RIGHT: c.rotate(90f); break;
            case DOWN: c.rotate(180f); break;
            case LEFT: c.rotate(270f); break;
            default: break;
        }
        float h = size * 0.5f;
        float w = size * 0.38f;
        Path p = new Path();
        p.moveTo(0f, -h);
        p.lineTo(w, -h * 0.36f);
        p.lineTo(w * 0.38f, -h * 0.36f);
        p.lineTo(w * 0.38f, h);
        p.lineTo(-w * 0.38f, h);
        p.lineTo(-w * 0.38f, -h * 0.36f);
        p.lineTo(-w, -h * 0.36f);
        p.close();
        PAINT.reset();
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(color);
        c.drawPath(p, PAINT);
        c.restore();
    }

    static void arrowOutline(Canvas c, float cx, float cy, float size, Direction dir, int color, float stroke) {
        if (dir == null) return;
        c.save();
        c.translate(cx, cy);
        switch (dir) {
            case RIGHT: c.rotate(90f); break;
            case DOWN: c.rotate(180f); break;
            case LEFT: c.rotate(270f); break;
            default: break;
        }
        float h = size * 0.5f;
        float w = size * 0.38f;
        Path p = new Path();
        p.moveTo(0f, -h);
        p.lineTo(w, -h * 0.36f);
        p.lineTo(w * 0.38f, -h * 0.36f);
        p.lineTo(w * 0.38f, h);
        p.lineTo(-w * 0.38f, h);
        p.lineTo(-w * 0.38f, -h * 0.36f);
        p.lineTo(-w, -h * 0.36f);
        p.close();
        PAINT.reset();
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(stroke);
        PAINT.setStrokeJoin(Paint.Join.MITER);
        PAINT.setColor(color);
        c.drawPath(p, PAINT);
        c.restore();
    }

    static void bracket(Canvas c, float l, float t, float r, float b, float len, int color, float stroke) {
        PAINT.reset();
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(stroke);
        PAINT.setColor(color);
        c.drawLine(l, t, l + len, t, PAINT);
        c.drawLine(l, t, l, t + len, PAINT);
        c.drawLine(r, b, r - len, b, PAINT);
        c.drawLine(r, b, r, b - len, PAINT);
    }

    static void background(Canvas c, float w, float h) {
        PAINT.reset();
        PAINT.setShader(new LinearGradient(0f, 0f, 0f, h,
                new int[]{0xFF020405, 0xFF071013, 0xFF020405},
                new float[]{0f, 0.45f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, w, h, PAINT);
        PAINT.setShader(null);
        // Faint tactical grid.
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(0.7f);
        PAINT.setColor(0x0DF1EEDC);
        for (float x = 0f; x <= w; x += 45f) c.drawLine(x, 0f, x, h, PAINT);
        for (float y = 0f; y <= h; y += 45f) c.drawLine(0f, y, w, y, PAINT);
        // Warm vignette at the top.
        PAINT.setShader(new LinearGradient(0f, 0f, 0f, 260f,
                0x18FFD24A, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(0f, 0f, w, 260f, PAINT);
        PAINT.setShader(null);
    }

    static void ring(Canvas c, float cx, float cy, float r, int color, float stroke) {
        PAINT.reset();
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(stroke);
        PAINT.setColor(color);
        c.drawCircle(cx, cy, r, PAINT);
    }

    static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = Color.red(a), ag = Color.green(a), ab = Color.blue(a), aa = Color.alpha(a);
        int br = Color.red(b), bg = Color.green(b), bb = Color.blue(b), ba = Color.alpha(b);
        return Color.argb((int) (aa + (ba - aa) * t), (int) (ar + (br - ar) * t),
                (int) (ag + (bg - ag) * t), (int) (ab + (bb - ab) * t));
    }

    static void progressBar(Canvas c, float l, float y, float r, float h, float frac, int color) {
        frac = Math.max(0f, Math.min(1f, frac));
        PAINT.reset();
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(0x33F1EEDC);
        c.drawRect(l, y, r, y + h, PAINT);
        PAINT.setColor(color);
        c.drawRect(l, y, l + (r - l) * frac, y + h, PAINT);
        // little segment ticks
        PAINT.setColor(0x66000000);
        for (float x = l + 12f; x < r; x += 26f) c.drawRect(x, y, x + 2f, y + h, PAINT);
    }

    static void slider(Canvas c, float l, float y, float r, float h, float frac, int color) {
        progressBar(c, l, y, r, h, frac, color);
        frac = Math.max(0f, Math.min(1f, frac));
        float cx = l + (r - l) * frac;
        float cy = y + h * 0.5f;
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(CREAM);
        c.drawCircle(cx, cy, h * 1.25f, PAINT);
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(1.4f);
        PAINT.setColor(color);
        c.drawCircle(cx, cy, h * 1.25f, PAINT);
    }

    static void button(Canvas c, String label, float l, float t, float r, float b,
                       boolean primary, boolean enabled) {
        int fill = primary ? (enabled ? GOLD : 0xFF3A3A2C) : 0xFF111A1C;
        int edge = primary ? (enabled ? GOLD_SOFT : 0xFF4B4B3C) : 0x66F1EEDC;
        int fg = primary ? (enabled ? 0xFF101313 : 0xFF77776A) : CREAM;
        panel(c, l, t, r, b, 9f, fill, edge);
        text(c, label, (l + r) * 0.5f, (t + b) * 0.5f + 6f, 17f, fg,
                Paint.Align.CENTER, BODY_BOLD);
    }

    static void toggle(Canvas c, float cx, float cy, boolean on, int accent) {
        float w = 40f;
        float h = 22f;
        float l = cx - w * 0.5f;
        float t = cy - h * 0.5f;
        float r = cx + w * 0.5f;
        float b = cy + h * 0.5f;
        PAINT.reset();
        PAINT.setAntiAlias(true);
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(on ? accent : 0xFF1A2528);
        c.drawRoundRect(l, t, r, b, h * 0.5f, h * 0.5f, PAINT);
        PAINT.setStyle(Paint.Style.STROKE);
        PAINT.setStrokeWidth(1.2f);
        PAINT.setColor(on ? GOLD_SOFT : 0x55F1EEDC);
        c.drawRoundRect(l, t, r, b, h * 0.5f, h * 0.5f, PAINT);
        PAINT.setStyle(Paint.Style.FILL);
        PAINT.setColor(on ? 0xFF101313 : 0xFF6E777A);
        c.drawCircle(cx + (on ? 9f : -9f), cy, 8f, PAINT);
    }

    static RectF rect(float l, float t, float r, float b) {
        RECT.set(l, t, r, b);
        return RECT;
    }

    static boolean inside(float x, float y, float l, float t, float r, float b) {
        return x >= l && x <= r && y >= t && y <= b;
    }
}
