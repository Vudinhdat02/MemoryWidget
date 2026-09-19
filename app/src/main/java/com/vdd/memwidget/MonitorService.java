package com.vdd.memwidget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;

/**
 * Giữ widget cập nhật trực tiếp.
 *
 * Tiết kiệm pin:
 *  - Chỉ chạy vòng lặp khi màn hình BẬT. Tắt màn hình là dừng hẳn, không còn đánh thức CPU.
 *  - Mỗi nhịp chỉ đọc RAM (nhẹ); dung lượng bộ nhớ trong được cache 15 giây.
 *  - Chỉ đẩy RemoteViews sang launcher khi con số hiển thị thực sự đổi.
 *  - Bật chế độ Tiết kiệm pin thì giãn nhịp ra 10 giây.
 */
public class MonitorService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "monitor";
    private static final long INTERVAL_MS = 3_000L;
    private static final long INTERVAL_POWER_SAVE_MS = 10_000L;

    private HandlerThread thread;
    private Handler handler;
    private PowerManager powerManager;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!WidgetUpdater.hasWidgets(MonitorService.this)) {
                stopSelf();
                return;
            }
            WidgetUpdater.updateAll(MonitorService.this, false);
            handler.postDelayed(this,
                    powerManager.isPowerSaveMode() ? INTERVAL_POWER_SAVE_MS : INTERVAL_MS);
        }
    };

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                resume();
            } else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                pause();
            }
        }
    };

    /** Khởi động service (best-effort: Android có thể từ chối nếu app đang ở nền). */
    static void start(Context context) {
        try {
            Context app = context.getApplicationContext();
            app.startForegroundService(new Intent(app, MonitorService.class));
        } catch (RuntimeException ignored) {
            // Không được phép khởi động từ nền lúc này -> lần sau (mở app, chạm widget, khởi động máy) sẽ chạy.
        }
    }

    static void stop(Context context) {
        Context app = context.getApplicationContext();
        app.stopService(new Intent(app, MonitorService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        powerManager = getSystemService(PowerManager.class);
        thread = new HandlerThread("memwidget-monitor", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        handler = new Handler(thread.getLooper());

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, null, handler, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter, null, handler);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Phải gọi startForeground ngay (trong vòng 5 giây kể từ startForegroundService).
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, n);
        }

        if (powerManager.isInteractive()) {
            resume();
        } else {
            pause();
        }
        return START_STICKY;
    }

    private void resume() {
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    private void pause() {
        handler.removeCallbacks(tick);
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, getString(R.string.monitor_channel), NotificationManager.IMPORTANCE_MIN);
        channel.setShowBadge(false);
        nm.createNotificationChannel(channel);

        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_mem)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.monitor_text))
                .setContentIntent(pi)
                .setOngoing(true)
                .setShowWhen(false)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        try {
            unregisterReceiver(screenReceiver);
        } catch (IllegalArgumentException ignored) {
            // chưa đăng ký
        }
        thread.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
