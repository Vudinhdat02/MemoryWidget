package com.vdd.memwidget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Vòng tròn phần trăm tối giản (nền + cung tiến trình bo tròn + số % ở giữa). */
public class RingView extends View {
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float stroke;

    private float shown;      // giá trị đang hiển thị (0..100) - dùng cho animation
    private int target = -1;  // giá trị đích
    private ValueAnimator animator;

    public RingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        stroke = 10f * density;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(stroke);

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 22, getResources().getDisplayMetrics()));
    }

    public void setColors(int track, int arc, int text) {
        trackPaint.setColor(track);
        arcPaint.setColor(arc);
        textPaint.setColor(text);
        invalidate();
    }

    public void setPercent(int percent) {
        int p = Math.max(0, Math.min(100, percent));
        if (p == target) return;
        boolean first = target < 0;
        target = p;
        if (animator != null) animator.cancel();
        if (first) {
            shown = 0f;
        }
        animator = ValueAnimator.ofFloat(shown, p);
        animator.setDuration(first ? 700 : 400);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float half = stroke / 2f;
        oval.set(getPaddingLeft() + half,
                getPaddingTop() + half,
                getWidth() - getPaddingRight() - half,
                getHeight() - getPaddingBottom() - half);

        canvas.drawArc(oval, 0f, 360f, false, trackPaint);
        if (shown > 0.5f) {
            canvas.drawArc(oval, -90f, 360f * shown / 100f, false, arcPaint);
        }

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(Math.round(shown) + "%", cx, cy, textPaint);
    }
}
