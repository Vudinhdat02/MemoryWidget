package com.vdd.memwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Sau khi khởi động máy hoặc cài bản mới của app: bật lại cập nhật trực tiếp cho widget. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!WidgetUpdater.hasWidgets(context)) return;
        WidgetUpdater.schedule(context);
        WidgetUpdater.updateAll(context);
        MonitorService.start(context);
    }
}
