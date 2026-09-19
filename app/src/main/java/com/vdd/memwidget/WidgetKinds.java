package com.vdd.memwidget;

import android.content.Context;
import android.widget.RemoteViews;

/** Danh sách mọi loại widget của app: provider, kích thước ô, cách dựng RemoteViews, khóa chống cập nhật thừa. */
final class WidgetKinds {
    static final int SQUARE_2X2 = 0;
    static final int BARS_2X4 = 1;
    static final int DEVICE_INFO = 2;
    static final int TERMINAL = 3;
    static final int RAM_GAUGE = 4;
    static final int STORAGE = 5;
    static final int BATTERY = 6;
    static final int RAM_CARD = 7;

    static final class Kind {
        final Class<? extends BaseWidgetProvider> provider;
        final int titleRes;
        final int cellsW;
        final int cellsH;
        /** Kích thước (dp) dùng khi launcher chưa báo cỡ thật. */
        final float defW;
        final float defH;
        /** Kích thước (dp) xem trước trong app - xấp xỉ một widget đặt trên màn hình chính. */
        final float prevW;
        final float prevH;

        Kind(Class<? extends BaseWidgetProvider> provider, int titleRes, int cellsW, int cellsH,
             float defW, float defH, float prevW, float prevH) {
            this.provider = provider;
            this.titleRes = titleRes;
            this.cellsW = cellsW;
            this.cellsH = cellsH;
            this.defW = defW;
            this.defH = defH;
            this.prevW = prevW;
            this.prevH = prevH;
        }
    }

    static final Kind[] ALL = {
            new Kind(Widget2x2Provider.class, R.string.widget_2x2_label, 2, 2, 120, 120, 150, 150),
            new Kind(Widget2x4Provider.class, R.string.widget_2x4_label, 4, 2, 300, 120, 320, 130),
            new Kind(DeviceInfoWidgetProvider.class, R.string.widget_deviceinfo_label, 4, 2, 300, 130, 320, 145),
            new Kind(TerminalWidgetProvider.class, R.string.widget_terminal_label, 4, 2, 300, 130, 320, 145),
            new Kind(RamGaugeWidgetProvider.class, R.string.widget_ramgauge_label, 2, 1, 150, 76, 160, 76),
            new Kind(StorageWidgetProvider.class, R.string.widget_storage_label, 2, 1, 150, 76, 160, 76),
            new Kind(BatteryWidgetProvider.class, R.string.widget_battery_label, 2, 1, 150, 76, 160, 76),
            new Kind(RamCardWidgetProvider.class, R.string.widget_ramcard_label, 2, 2, 150, 150, 160, 160),
    };

    private WidgetKinds() {}

    static RemoteViews build(Context ctx, int kind, Snapshot s, float w, float h, boolean preview) {
        RemoteViews rv;
        switch (kind) {
            case SQUARE_2X2: rv = WidgetViews.square2x2(ctx, s, w, h, preview); break;
            case BARS_2X4: rv = WidgetViews.bars2x4(ctx, s, w, h, preview); break;
            case DEVICE_INFO: rv = WidgetViews.deviceInfo(ctx, s, w, h, preview); break;
            case TERMINAL: rv = WidgetViews.terminal(ctx, s, w, h, preview); break;
            case RAM_GAUGE: rv = WidgetViews.ramGauge(ctx, s, w, h, preview); break;
            case STORAGE: rv = WidgetViews.storageRing(ctx, s, w, h, preview); break;
            case BATTERY: rv = WidgetViews.battery(ctx, s, w, h, preview); break;
            case RAM_CARD: rv = WidgetViews.ramCard(ctx, s, w, h, preview); break;
            default: throw new IllegalArgumentException("kind " + kind);
        }
        WidgetStyle.apply(ctx, rv, kind); // nền đặc / kính + độ trong suốt do người dùng chọn
        return rv;
    }

    /**
     * Dấu vân tay của những gì widget này đang hiển thị. Không đổi = không cần đẩy lại RemoteViews
     * (đỡ tốn pin). Chỉ gồm đúng các con số xuất hiện trên widget đó.
     */
    static String key(int kind, Snapshot s) {
        MemStats m = s.mem;
        BatteryInfo b = s.bat;
        switch (kind) {
            case SQUARE_2X2:
                return m.ramPercent + ":" + m.romPercent;
            case BARS_2X4:
                return m.ramPercent + ":" + m.romPercent + ":" + Fmt.size(m.ramUsed, true)
                        + ":" + Fmt.size(m.romUsed, false);
            case DEVICE_INFO:
                return m.ramPercent + ":" + Fmt.gbFixed(m.ramUsed, true, 2) + ":" + Fmt.gbFixed(m.romUsed, false, 2)
                        + ":" + m.romPercent + ":" + b.percent + ":" + b.voltageMv + ":" + Math.round(b.tempC)
                        + ":" + b.isPowered();
            case TERMINAL:
                return b.percent + ":" + Math.round(b.tempC * 10f) + ":" + b.statusWord()
                        + ":" + Fmt.gbTrim(m.romUsed, false, 1) + ":" + m.romPercent;
            case RAM_GAUGE:
                return m.ramPercent + ":" + Fmt.gbFixed(m.ramUsed, true, 2);
            case STORAGE:
                return m.romPercent + ":" + Fmt.gbFixed(m.romFree, false, 2);
            case BATTERY:
                return b.percent + ":" + Math.round(b.tempC * 10f) + ":" + b.isPowered();
            case RAM_CARD:
                return m.ramPercent + ":" + Fmt.gbFixed(m.ramAvail, true, 1);
            default:
                return "";
        }
    }
}
