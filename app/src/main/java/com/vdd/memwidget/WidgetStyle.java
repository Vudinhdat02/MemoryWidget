package com.vdd.memwidget;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * Tuỳ chọn giao diện chung cho mọi widget: độ trong suốt của nền và hiệu ứng kính (Liquid Glass).
 * Lưu trong SharedPreferences; áp dụng lên ảnh nền {@code widget_bg} của từng layout widget.
 */
final class WidgetStyle {
    private static final String PREFS = "widget_style";
    private static final String KEY_TRANSPARENCY = "transparency"; // 0 = đục hẳn ... 100 = trong hẳn
    private static final String KEY_GLASS = "glass";

    static final int MAX_TRANSPARENCY = 100;

    /** Nền đặc / nền kính cho từng loại widget, cùng thứ tự với {@link WidgetKinds#ALL}. */
    private static final int[] SOLID = {
            R.drawable.widget_bg,           // SQUARE_2X2
            R.drawable.widget_bg,           // BARS_2X4
            R.drawable.widget_card_bg,      // DEVICE_INFO
            R.drawable.widget_terminal_bg,  // TERMINAL
            R.drawable.widget_pill_bg,      // RAM_GAUGE
            R.drawable.widget_pill_bg,      // STORAGE
            R.drawable.widget_pill_bg,      // BATTERY
            R.drawable.widget_card_bg,      // RAM_CARD
    };
    private static final int[] GLASS = {
            R.drawable.widget_card_glass,
            R.drawable.widget_card_glass,
            R.drawable.widget_card_glass,
            R.drawable.widget_terminal_glass,
            R.drawable.widget_pill_glass,
            R.drawable.widget_pill_glass,
            R.drawable.widget_pill_glass,
            R.drawable.widget_card_glass,
    };

    private WidgetStyle() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Độ trong suốt 0..100 (%). */
    static int transparency(Context ctx) {
        int v = prefs(ctx).getInt(KEY_TRANSPARENCY, 0);
        return Math.max(0, Math.min(MAX_TRANSPARENCY, v));
    }

    static boolean glass(Context ctx) {
        return prefs(ctx).getBoolean(KEY_GLASS, false);
    }

    static void setTransparency(Context ctx, int percent) {
        prefs(ctx).edit().putInt(KEY_TRANSPARENCY, Math.max(0, Math.min(MAX_TRANSPARENCY, percent))).apply();
    }

    static void setGlass(Context ctx, boolean on) {
        prefs(ctx).edit().putBoolean(KEY_GLASS, on).apply();
    }

    /** Thay đổi tuỳ chọn -> phải vẽ lại widget (đưa vào khóa chống cập nhật thừa). */
    static String key(Context ctx) {
        return transparency(ctx) + (glass(ctx) ? "g" : "s");
    }

    /** Gắn nền (đặc hoặc kính) và độ trong suốt cho ảnh nền {@code widget_bg} của widget. */
    static void apply(Context ctx, RemoteViews rv, int kind) {
        int res = glass(ctx) ? GLASS[kind] : SOLID[kind];
        rv.setImageViewResource(R.id.widget_bg, res);
        int alpha = Math.round(255f * (MAX_TRANSPARENCY - transparency(ctx)) / MAX_TRANSPARENCY);
        rv.setInt(R.id.widget_bg, "setImageAlpha", alpha);
    }
}
