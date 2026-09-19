package com.vdd.memwidget;

import android.content.Context;

/** Toàn bộ số liệu (RAM, bộ nhớ trong, pin) tại một thời điểm - dùng chung cho mọi widget. */
final class Snapshot {
    final MemStats mem;
    final BatteryInfo bat;

    private Snapshot(MemStats mem, BatteryInfo bat) {
        this.mem = mem;
        this.bat = bat;
    }

    /** @param maxStorageAgeMs tuổi tối đa của số liệu bộ nhớ trong được phép dùng lại (0 = đọc mới). */
    static Snapshot read(Context context, long maxStorageAgeMs) {
        Context app = context.getApplicationContext();
        return new Snapshot(MemStats.readLive(app, maxStorageAgeMs), BatteryInfo.read(app));
    }
}
