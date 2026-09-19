package com.vdd.memwidget;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

/** Ảnh chụp trạng thái pin (đọc từ broadcast "dính" ACTION_BATTERY_CHANGED, không cần quyền, rất nhẹ). */
final class BatteryInfo {
    int percent = 0;
    boolean charging;
    boolean full;
    boolean plugged;
    float tempC;
    int voltageMv;
    private int status = BatteryManager.BATTERY_STATUS_UNKNOWN;

    static BatteryInfo read(Context context) {
        BatteryInfo b = new BatteryInfo();
        Intent i = context.getApplicationContext()
                .registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return b;
        int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        if (level >= 0 && scale > 0) b.percent = Math.max(0, Math.min(100, Math.round(level * 100f / scale)));
        b.status = i.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
        b.charging = b.status == BatteryManager.BATTERY_STATUS_CHARGING;
        b.full = b.status == BatteryManager.BATTERY_STATUS_FULL;
        b.plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        b.tempC = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        b.voltageMv = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        return b;
    }

    /** Đang cắm sạc (kể cả đầy pin nhưng vẫn cắm). */
    boolean isPowered() {
        return charging || plugged;
    }

    /** Từ khóa kiểu terminal: charging / discharging / full / not charging. */
    String statusWord() {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "not charging";
            default: return plugged ? "charging" : "discharging";
        }
    }
}
