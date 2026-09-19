package com.vdd.memwidget;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.Locale;

/**
 * Dựng RemoteViews cho từng loại widget. Mọi kích thước (dp) được tính theo cỡ thật của widget
 * (w x h) nên widget co giãn đẹp trên nhiều launcher. {@code preview = true}: dùng để xem trước trong app
 * (không gắn thao tác chạm).
 */
final class WidgetViews {
    private static final int DIP = TypedValue.COMPLEX_UNIT_DIP;
    private static final int SP = TypedValue.COMPLEX_UNIT_SP;
    private static final int FLAGS = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

    /** Cung đo RAM: hở đáy 108°, bắt đầu 144° (~7 giờ 30), quét 252°. */
    private static final float GAUGE_START = 144f;
    private static final float GAUGE_SWEEP = 252f;

    private WidgetViews() {}

    // ------------------------------------------------------------------ tiện ích

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static float scale(float w, float h, float refW, float refH) {
        return clamp(Math.min(w / refW, h / refH), 0.7f, 1.5f);
    }

    /** Cỡ chữ (sp) tối đa sao cho {@code text} vừa bề ngang {@code availDp}. */
    private static float fitSp(String text, float availDp, float maxSp) {
        float perChar = 0.58f; // bề ngang trung bình 1 ký tự / cỡ chữ
        return Math.max(9f, Math.min(maxSp, availDp / (Math.max(1, text.length()) * perChar)));
    }

    private static void box(RemoteViews rv, int id, float w, float h) {
        rv.setViewLayoutWidth(id, w, DIP);
        rv.setViewLayoutHeight(id, h, DIP);
    }

    private static void sp(RemoteViews rv, int id, float size) {
        rv.setTextViewTextSize(id, SP, size);
    }

    private static void dp(RemoteViews rv, int id, float size) {
        rv.setTextViewTextSize(id, DIP, size);
    }

    private static void sp(RemoteViews rv, int[] ids, float size) {
        for (int id : ids) rv.setTextViewTextSize(id, SP, size);
    }

    private static void tapToOpen(Context ctx, RemoteViews rv, boolean preview) {
        if (preview) return;
        Intent open = new Intent(ctx, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        rv.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, 0, open, FLAGS));
    }

    private static int track(Context ctx) {
        return ctx.getColor(R.color.w_track);
    }

    private static int green(Context ctx) {
        return ctx.getColor(R.color.w_green);
    }

    private static String gbPair(long used, long total, boolean binary, String sep) {
        return Fmt.gbFixed(used, binary, 2) + "G" + sep + Fmt.gbFixed(total, binary, 2) + "G";
    }

    // ------------------------------------------------------------------ 2x2 (vuông) - widget gốc

    static RemoteViews square2x2(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_2x2);
        MemStats m = s.mem;

        rv.setProgressBar(R.id.ram_progress, 100, m.ramPercent, false);
        rv.setProgressBar(R.id.rom_progress, 100, m.romPercent, false);
        rv.setTextViewText(R.id.ram_percent, m.ramPercent + "%");
        rv.setTextViewText(R.id.rom_percent, m.romPercent + "%");
        tapToOpen(ctx, rv, preview);

        // Hình vuông đúng nghĩa: cạnh = cạnh ngắn hơn của ô. Lề ngoài = khoảng giữa 2 hàng = g,
        // và g + d + g + d + g = cạnh.
        float side = Math.max(60f, Math.min(w, h));
        float g = side * 0.11f;
        float d = (side - 3f * g) / 2f;
        float labelGap = g * 0.7f;
        int gPx = Math.round(TypedValue.applyDimension(DIP, g, ctx.getResources().getDisplayMetrics()));

        box(rv, R.id.widget_root, side, side);
        rv.setViewPadding(R.id.widget_root, gPx, gPx, gPx, gPx);
        box(rv, R.id.rom_box, d, d);
        box(rv, R.id.ram_box, d, d);
        rv.setViewLayoutMargin(R.id.row_ram, RemoteViews.MARGIN_TOP, g, DIP);
        rv.setViewLayoutMargin(R.id.rom_label, RemoteViews.MARGIN_START, labelGap, DIP);
        rv.setViewLayoutMargin(R.id.ram_label, RemoteViews.MARGIN_START, labelGap, DIP);

        float percentSize = clamp(d * 0.27f, 9f, 16f);
        dp(rv, R.id.rom_percent, percentSize);
        dp(rv, R.id.ram_percent, percentSize);
        float labelSize = clamp(side * 0.082f, 10.5f, 12.5f);
        sp(rv, new int[] {R.id.rom_label, R.id.ram_label}, labelSize);
        return rv;
    }

    // ------------------------------------------------------------------ 2x4 (hai thanh + nút dọn RAM)

    static RemoteViews bars2x4(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_2x4);
        MemStats m = s.mem;

        rv.setProgressBar(R.id.ram_progress, 100, m.ramPercent, false);
        rv.setProgressBar(R.id.rom_progress, 100, m.romPercent, false);
        rv.setTextViewText(R.id.ram_percent, m.ramPercent + "%");
        rv.setTextViewText(R.id.rom_percent, m.romPercent + "%");
        rv.setTextViewText(R.id.ram_detail, Fmt.size(m.ramUsed, true) + " / " + Fmt.size(m.ramTotal, true));
        rv.setTextViewText(R.id.rom_detail, Fmt.size(m.romUsed, false) + " / " + Fmt.size(m.romTotal, false));
        tapToOpen(ctx, rv, preview);
        if (!preview) {
            Intent clean = new Intent(ctx, WidgetActionReceiver.class).setAction(WidgetUpdater.ACTION_CLEAN);
            rv.setOnClickPendingIntent(R.id.btn_clean, PendingIntent.getBroadcast(ctx, 1, clean, FLAGS));
        }
        return rv;
    }

    // ------------------------------------------------------------------ Thông tin thiết bị 4x2

    static RemoteViews deviceInfo(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_device_info);
        MemStats m = s.mem;
        BatteryInfo b = s.bat;
        // Chiều cao ruột (trừ padding 10dp x 2) chia theo tỉ lệ 4.6 : 5.4 giữa hàng đồng hồ và hàng RAM/thanh.
        float inner = h - 20f;
        float topH = inner * 0.46f;
        float lowH = inner * 0.54f;
        float clock = clamp(topH * 0.64f, 22f, 46f);
        sp(rv, R.id.di_time, clock);
        sp(rv, R.id.di_date, clamp(clock * 0.38f, 11f, 16f));

        float gd = Math.max(40f, Math.min(lowH * 0.97f, (w - 32f) * 0.30f));
        box(rv, R.id.di_gauge_box, gd, gd);
        rv.setImageViewBitmap(R.id.di_gauge, Gauges.arc(ctx, gd, 1f, GAUGE_START, GAUGE_SWEEP,
                m.ramPercent / 100f, gd * 0.05f, track(ctx), green(ctx)));
        dp(rv, R.id.di_ram_percent, gd * 0.21f);
        dp(rv, R.id.di_ram_detail, Math.max(6f, gd * 0.095f));
        dp(rv, R.id.di_ram_label, gd * 0.15f);
        rv.setTextViewText(R.id.di_ram_percent, m.ramPercent + "%");
        rv.setTextViewText(R.id.di_ram_detail, gbPair(m.ramUsed, m.ramTotal, true, "/"));

        rv.setTextViewText(R.id.di_storage_detail, gbPair(m.romUsed, m.romTotal, false, " / "));
        rv.setTextViewText(R.id.di_storage_percent, m.romPercent + "%");
        rv.setProgressBar(R.id.di_storage_bar, 100, m.romPercent, false);

        rv.setTextViewText(R.id.di_battery_label,
                ctx.getString(b.isPowered() ? R.string.di_battery_charging : R.string.di_battery));
        rv.setTextViewText(R.id.di_battery_detail, b.voltageMv + "mV / " + Math.round(b.tempC) + "°C");
        rv.setTextViewText(R.id.di_battery_percent, b.percent + "%");
        rv.setProgressBar(R.id.di_battery_bar, 100, b.percent, false);

        sp(rv, new int[] {R.id.di_storage_label, R.id.di_storage_detail, R.id.di_storage_percent,
                R.id.di_battery_label, R.id.di_battery_detail, R.id.di_battery_percent},
                clamp(lowH * 0.165f, 8f, 14f));
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    // ------------------------------------------------------------------ Kiểu terminal 4x2

    static RemoteViews terminal(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_terminal);
        MemStats m = s.mem;
        BatteryInfo b = s.bat;
        float u = scale(w, h, 320f, 145f);

        rv.setTextViewText(R.id.tm_dev_val, DeviceInfo.fullName(ctx));
        rv.setTextViewText(R.id.tm_sys_val, DeviceInfo.androidLine());
        rv.setTextViewText(R.id.tm_soc_val, DeviceInfo.socLine());
        rv.setTextViewText(R.id.tm_bat_val, b.percent + "% "
                + String.format(Locale.getDefault(), "%.1f", b.tempC) + "°C " + b.statusWord());
        rv.setTextViewText(R.id.tm_dsk_val, Fmt.gbTrim(m.romUsed, false, 1) + "G / "
                + Fmt.gbTrim(m.romTotal, false, 1) + "G (" + m.romPercent + "%)");
        rv.setTextViewText(R.id.tm_prompt, "DevInfo@" + DeviceInfo.name(ctx) + ":~$ _");

        sp(rv, new int[] {
                R.id.tm_dev_key, R.id.tm_sys_key, R.id.tm_soc_key, R.id.tm_bat_key, R.id.tm_dsk_key,
                R.id.tm_dev_colon, R.id.tm_sys_colon, R.id.tm_soc_colon, R.id.tm_bat_colon, R.id.tm_dsk_colon,
                R.id.tm_dev_val, R.id.tm_sys_val, R.id.tm_soc_val, R.id.tm_bat_val, R.id.tm_dsk_val,
                R.id.tm_prompt}, 12f * u);
        sp(rv, R.id.tm_title, 11f * u);
        for (int id : new int[] {R.id.tm_dot1, R.id.tm_dot2, R.id.tm_dot3}) box(rv, id, 10f * u, 10f * u);
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    // ------------------------------------------------------------------ RAM sử dụng 2x1

    static RemoteViews ramGauge(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_ram_gauge);
        MemStats m = s.mem;

        float b = Math.max(40f, Math.min(w, h) * 0.88f);
        box(rv, R.id.rg_box, b, b);
        rv.setImageViewBitmap(R.id.rg_gauge, Gauges.arc(ctx, b, 1f, GAUGE_START, GAUGE_SWEEP,
                m.ramPercent / 100f, b * 0.055f, track(ctx), green(ctx)));
        dp(rv, R.id.rg_percent, b * 0.20f);
        dp(rv, R.id.rg_detail, Math.max(6f, b * 0.093f));
        dp(rv, R.id.rg_label, Math.max(8f, b * 0.135f));
        rv.setTextViewText(R.id.rg_percent, m.ramPercent + "%");
        rv.setTextViewText(R.id.rg_detail, gbPair(m.ramUsed, m.ramTotal, true, "/"));
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    // ------------------------------------------------------------------ Bộ nhớ trong 2x1

    static RemoteViews storageRing(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_storage_ring);
        MemStats m = s.mem;
        float u = scale(w, h, 160f, 76f);

        float r = clamp(h * 0.74f, 30f, 64f);
        box(rv, R.id.sr_ring, r, r);
        rv.setImageViewBitmap(R.id.sr_ring, Gauges.arc(ctx, r, 1f, 270f, 360f,
                m.romPercent / 100f, r * 0.20f, track(ctx), green(ctx)));

        String value = Fmt.gbFixed(m.romFree, false, 2) + " GB";
        float avail = w - 12f - 14f - r - 10f;
        String caption = ctx.getString(R.string.sr_caption);
        rv.setTextViewText(R.id.sr_value, value);
        sp(rv, R.id.sr_value, fitSp(value, avail, 19f * u));
        sp(rv, R.id.sr_caption, fitSp(caption, avail, Math.min(11f * u, 13f)));
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    // ------------------------------------------------------------------ Pin 2x1

    static RemoteViews battery(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_battery);
        BatteryInfo b = s.bat;
        float u = scale(w, h, 160f, 76f);

        float r = clamp(h * 0.74f, 30f, 64f);
        box(rv, R.id.bt_ring, r, r);
        rv.setImageViewBitmap(R.id.bt_ring, Gauges.batteryRing(ctx, r, b.percent / 100f, b.isPowered(),
                r * 0.10f, track(ctx), green(ctx), ctx.getColor(R.color.w_text_sub)));

        String title = ctx.getString(R.string.bt_title, b.percent);
        String temp = ctx.getString(R.string.bt_temp, String.format(Locale.getDefault(), "%.1f", b.tempC));
        float avail = w - 12f - 14f - r - 10f;
        rv.setTextViewText(R.id.bt_title, title);
        rv.setTextViewText(R.id.bt_temp, temp);
        float textSize = Math.min(fitSp(title, avail, 14f * u), fitSp(temp, avail, 14f * u));
        sp(rv, R.id.bt_title, textSize);
        sp(rv, R.id.bt_temp, textSize);
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    // ------------------------------------------------------------------ RAM chi tiết 2x2

    static RemoteViews ramCard(Context ctx, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_ram_card);
        MemStats m = s.mem;
        float u = scale(w, h, 150f, 150f);

        final float pad = 12f;
        final float cropHeight = 0.64f;
        float rowH = 34f * u;
        float b = Math.max(70f, Math.min(w - 2f * pad, (h - 2f * pad - 6f - rowH) / cropHeight));
        // Cung hở đáy: bắt đầu 170°, quét 200°.
        box(rv, R.id.rc_arc_box, b, b * cropHeight);
        rv.setImageViewBitmap(R.id.rc_gauge, Gauges.arc(ctx, b, cropHeight, 170f, 200f,
                m.ramPercent / 100f, b * 0.075f, track(ctx), green(ctx)));
        box(rv, R.id.rc_icon, b * 0.13f, b * 0.13f);
        dp(rv, R.id.rc_percent, b * 0.24f);
        rv.setTextViewText(R.id.rc_percent, m.ramPercent + "%");

        rv.setTextViewText(R.id.rc_total_value, withSmallUnit(Fmt.gbFixed(m.ramTotal, true, 1) + "GB"));
        rv.setTextViewText(R.id.rc_avail_value, withSmallUnit(Fmt.gbFixed(m.ramAvail, true, 1) + "GB"));
        sp(rv, new int[] {R.id.rc_total_value, R.id.rc_avail_value}, 16f * u);
        sp(rv, new int[] {R.id.rc_total_label, R.id.rc_avail_label}, 10f * u);
        tapToOpen(ctx, rv, preview);
        return rv;
    }

    /** "23.5GB" -> "GB" nhỏ hơn số. */
    private static CharSequence withSmallUnit(String text) {
        SpannableString ss = new SpannableString(text);
        if (text.length() > 2) {
            ss.setSpan(new RelativeSizeSpan(0.65f), text.length() - 2, text.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }
}
