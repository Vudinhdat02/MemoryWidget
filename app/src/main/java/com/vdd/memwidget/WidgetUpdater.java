package com.vdd.memwidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Đẩy cập nhật cho mọi widget (chỉ những widget có số liệu / kích thước / giao diện thực sự đổi). */
final class WidgetUpdater {
    static final String ACTION_REFRESH = "com.vdd.memwidget.action.REFRESH";
    static final String ACTION_CLEAN = "com.vdd.memwidget.action.CLEAN";

    private static final String TAG = "MemWidget";
    /** Báo thức dự phòng, phòng khi service nền bị hệ thống dừng. Không đánh thức máy. */
    private static final long FALLBACK_INTERVAL_MS = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    /** Dung lượng bộ nhớ trong đổi chậm nên chỉ hỏi lại hệ thống mỗi 15 giây. */
    private static final long STORAGE_MAX_AGE_MS = 15_000L;
    private static final int FLAGS = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;

    /** Dấu vân tay lần cập nhật gần nhất của từng widget (theo appWidgetId), để bỏ qua lần đẩy trùng. */
    private static final Map<Integer, String> lastKeys = new HashMap<>();

    private WidgetUpdater() {}

    static boolean hasWidgets(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);
        for (WidgetKinds.Kind kind : WidgetKinds.ALL) {
            if (mgr.getAppWidgetIds(new ComponentName(app, kind.provider)).length > 0) return true;
        }
        return false;
    }

    /** Cập nhật cưỡng bức (khi người dùng mở app, thêm widget, đổi kích thước, dọn RAM...). */
    static void updateAll(Context context) {
        updateAll(context, true);
    }

    /**
     * @param force false = bỏ qua widget nào không có gì thay đổi so với lần trước (dùng cho vòng lặp nền).
     */
    static synchronized void updateAll(Context context, boolean force) {
        Context app = context.getApplicationContext();
        AppWidgetManager mgr = AppWidgetManager.getInstance(app);

        int count = WidgetKinds.ALL.length;
        int[][] ids = new int[count][];
        boolean any = false;
        for (int i = 0; i < count; i++) {
            ids[i] = mgr.getAppWidgetIds(new ComponentName(app, WidgetKinds.ALL[i].provider));
            any |= ids[i].length > 0;
        }
        if (!any) {
            lastKeys.clear();
            return;
        }

        Snapshot snap = Snapshot.read(app, STORAGE_MAX_AGE_MS);
        String theme = themeKey(app);
        Set<Integer> alive = new HashSet<>();

        for (int i = 0; i < count; i++) {
            WidgetKinds.Kind kind = WidgetKinds.ALL[i];
            String dataKey = ids[i].length > 0 ? WidgetKinds.key(i, snap) : "";
            for (int id : ids[i]) {
                alive.add(id);
                float[] size = sizeDp(app, mgr, id, kind.defW, kind.defH);
                String key = i + "|" + dataKey + "|" + Math.round(size[0]) + "x" + Math.round(size[1]) + "|" + theme;
                if (!force && key.equals(lastKeys.get(id))) continue;
                try {
                    mgr.updateAppWidget(id, WidgetKinds.build(app, i, snap, size[0], size[1], false));
                    lastKeys.put(id, key);
                } catch (RuntimeException e) {
                    // Một widget lỗi không được làm hỏng các widget còn lại.
                    Log.w(TAG, "update widget " + id + " (kind " + i + ") failed", e);
                }
            }
        }
        lastKeys.keySet().retainAll(alive);
    }

    /** Thay đổi giao diện (sáng/tối, màu nền hệ thống) -> phải vẽ lại các bitmap. */
    private static String themeKey(Context ctx) {
        int night = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night + ":" + ctx.getColor(R.color.card_bg) + ":" + ctx.getColor(R.color.text_primary);
    }

    /** Kích thước (dp) launcher cấp cho widget, theo hướng màn hình hiện tại. */
    private static float[] sizeDp(Context ctx, AppWidgetManager mgr, int appWidgetId, float defW, float defH) {
        Bundle o = mgr.getAppWidgetOptions(appWidgetId);
        float minW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        float maxW = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0);
        float minH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        float maxH = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0);

        boolean landscape = ctx.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        // Dọc: rộng = min, cao = max. Ngang: rộng = max, cao = min.
        float w = landscape ? maxW : minW;
        float h = landscape ? minH : maxH;
        if (w <= 0) w = Math.max(minW, maxW);
        if (h <= 0) h = Math.max(minH, maxH);
        if (w < 40f) w = defW;
        if (h < 30f) h = defH;
        return new float[] {w, h};
    }

    /** Hẹn giờ dự phòng (không đánh thức máy). Việc cập nhật trực tiếp do MonitorService lo. */
    static void schedule(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + FALLBACK_INTERVAL_MS,
                FALLBACK_INTERVAL_MS,
                refreshIntent(context));
    }

    /** Huỷ hẹn giờ và dừng service nền khi không còn widget nào trên màn hình. */
    static void cancelIfUnused(Context context) {
        Context app = context.getApplicationContext();
        if (hasWidgets(app)) return;
        AlarmManager am = app.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(refreshIntent(app));
        MonitorService.stop(app);
    }

    private static PendingIntent refreshIntent(Context ctx) {
        Intent i = new Intent(ctx, WidgetActionReceiver.class).setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(ctx, 2, i, FLAGS);
    }
}
