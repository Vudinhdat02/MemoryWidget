package com.vdd.memwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Vẽ các vòng / cung tiến trình có đầu bo tròn thành Bitmap (RemoteViews không có view tự vẽ,
 * còn shape "ring" của Android thì không bo đầu được).
 */
final class Gauges {
    private Gauges() {}

    /**
     * Cung tròn tiến trình.
     *
     * @param diameterDp   đường kính vòng tròn (dp)
     * @param cropHeight   tỉ lệ chiều cao bitmap so với đường kính (1 = cả vòng; 0.62 = chỉ nửa trên, dùng cho cung hở đáy)
     * @param startDeg     góc bắt đầu, tính theo chiều kim đồng hồ từ hướng 3 giờ (270 = 12 giờ)
     * @param sweepDeg     tổng góc quét của nền (360 = vòng kín)
     * @param fraction     0..1 phần đã dùng
     * @param strokeDp     độ dày nét (dp)
     */
    static Bitmap arc(Context ctx, float diameterDp, float cropHeight, float startDeg, float sweepDeg,
                      float fraction, float strokeDp, int trackColor, int fillColor) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int size = Math.max(2, Math.round(diameterDp * density));
        int height = Math.max(2, Math.round(size * cropHeight));
        Bitmap bmp = Bitmap.createBitmap(size, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        float stroke = Math.max(1f, strokeDp * density);
        RectF oval = new RectF(stroke / 2f, stroke / 2f, size - stroke / 2f, size - stroke / 2f);
        drawArcs(c, oval, stroke, startDeg, sweepDeg, fraction, trackColor, fillColor);
        return bmp;
    }

    /** Vòng pin: vòng kín bắt đầu từ 12 giờ + biểu tượng viên pin ở giữa (có tia sét khi đang sạc). */
    static Bitmap batteryRing(Context ctx, float diameterDp, float fraction, boolean charging,
                              float strokeDp, int trackColor, int fillColor, int glyphColor) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int size = Math.max(2, Math.round(diameterDp * density));
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        float stroke = Math.max(1f, strokeDp * density);
        RectF oval = new RectF(stroke / 2f, stroke / 2f, size - stroke / 2f, size - stroke / 2f);
        drawArcs(c, oval, stroke, 270f, 360f, fraction, trackColor, fillColor);

        // Viên pin nằm giữa vòng
        float bodyW = size * 0.28f;
        float bodyH = size * 0.42f;
        float left = (size - bodyW) / 2f;
        float top = (size - bodyH) / 2f + size * 0.025f;
        float line = Math.max(1.5f, size * 0.05f);
        float radius = size * 0.05f;

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(glyphColor);

        // đầu cực
        float capW = bodyW * 0.42f;
        float capH = size * 0.05f;
        p.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF((size - capW) / 2f, top - capH, (size + capW) / 2f, top + 1f),
                capH / 2f, capH / 2f, p);

        // thân (viền)
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(line);
        RectF body = new RectF(left + line / 2f, top + line / 2f, left + bodyW - line / 2f, top + bodyH - line / 2f);
        c.drawRoundRect(body, radius, radius, p);

        // phần ruột theo mức pin (từ đáy đi lên)
        float inset = line * 1.6f;
        float innerL = left + inset;
        float innerR = left + bodyW - inset;
        float innerB = top + bodyH - inset;
        float innerT = top + inset;
        float fillTop = innerB - (innerB - innerT) * Math.max(0f, Math.min(1f, fraction));
        p.setStyle(Paint.Style.FILL);
        p.setAlpha(150);
        c.drawRoundRect(new RectF(innerL, fillTop, innerR, innerB), radius * 0.4f, radius * 0.4f, p);
        p.setAlpha(255);

        if (charging) {
            float cx = size / 2f;
            float cy = top + bodyH / 2f;
            float u = size * 0.16f;
            Path bolt = new Path();
            bolt.moveTo(cx + 0.10f * u, cy - 1.0f * u);
            bolt.lineTo(cx - 0.75f * u, cy + 0.15f * u);
            bolt.lineTo(cx - 0.05f * u, cy + 0.15f * u);
            bolt.lineTo(cx - 0.10f * u, cy + 1.0f * u);
            bolt.lineTo(cx + 0.75f * u, cy - 0.15f * u);
            bolt.lineTo(cx + 0.05f * u, cy - 0.15f * u);
            bolt.close();
            Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
            bp.setStyle(Paint.Style.FILL);
            bp.setColor(fillColor);
            c.drawPath(bolt, bp);
        }
        return bmp;
    }

    private static void drawArcs(Canvas c, RectF oval, float stroke, float startDeg, float sweepDeg,
                                 float fraction, int trackColor, int fillColor) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(stroke);
        p.setStrokeCap(Paint.Cap.ROUND);

        p.setColor(trackColor);
        c.drawArc(oval, startDeg, sweepDeg, false, p);

        float f = Math.max(0f, Math.min(1f, fraction));
        if (f > 0f) {
            p.setColor(fillColor);
            c.drawArc(oval, startDeg, Math.max(0.5f, sweepDeg * f), false, p);
        }
    }
}
