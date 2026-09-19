package com.vdd.memwidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

/** Xử lý nút "Dọn RAM" trên widget và lệnh làm mới định kỳ. */
public class WidgetActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (WidgetUpdater.ACTION_CLEAN.equals(action)) {
            final PendingResult pending = goAsync();
            final Context app = context.getApplicationContext();
            new Thread(() -> {
                try {
                    MemCleaner.Result r = MemCleaner.clean(app);
                    WidgetUpdater.updateAll(app);
                    final String msg = cleanMessage(app, r);
                    new Handler(Looper.getMainLooper()).post(
                            () -> Toast.makeText(app, msg, Toast.LENGTH_SHORT).show());
                } finally {
                    pending.finish();
                }
            }).start();
        } else if (WidgetUpdater.ACTION_REFRESH.equals(action)) {
            // Báo thức dự phòng: làm mới và thử bật lại service nền nếu nó đã bị dừng.
            WidgetUpdater.updateAll(context, false);
            MonitorService.start(context);
        }
    }

    static String cleanMessage(Context ctx, MemCleaner.Result r) {
        long total = r.freedBytes + r.ownCacheBytes;
        if (total >= 1_000_000L) {
            return ctx.getString(R.string.clean_done, Fmt.size(total, true));
        }
        return ctx.getString(R.string.clean_none);
    }
}
