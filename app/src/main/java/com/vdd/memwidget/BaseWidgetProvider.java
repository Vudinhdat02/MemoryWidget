package com.vdd.memwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.Bundle;

/** Hành vi chung cho cả widget 2x2 và 2x4. */
public abstract class BaseWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        WidgetUpdater.updateAll(context);
        WidgetUpdater.schedule(context);
        MonitorService.start(context);
    }

    /** Launcher báo kích thước ô thay đổi (đặt widget, xoay màn hình...) -> vẽ lại cho vuông. */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, Bundle newOptions) {
        WidgetUpdater.updateAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.schedule(context);
        MonitorService.start(context);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetUpdater.cancelIfUnused(context);
    }
}
