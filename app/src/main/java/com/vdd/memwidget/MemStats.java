package com.vdd.memwidget;

import android.app.ActivityManager;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/** Ảnh chụp nhanh số liệu RAM và ROM tại một thời điểm. */
final class MemStats {
    // RAM
    long ramTotal;
    long ramAvail;
    long ramUsed;
    int ramPercent;
    long ramCached;
    long swapTotal;
    long swapFree;
    boolean lowMemory;

    // ROM (bộ nhớ trong)
    long romTotal;
    long romFree;
    long romUsed;
    int romPercent;

    static MemStats read(Context context) {
        MemStats s = new MemStats();
        readRam(context, s);
        readProcMeminfo(s);
        readStorage(context, s);
        return s;
    }

    // Cache dung lượng bộ nhớ trong (đổi rất chậm, hỏi hệ thống mỗi nhịp là phí pin)
    private static long cachedRomTotal;
    private static long cachedRomFree;
    private static long cachedRomAt = -1L;

    /**
     * Bản nhẹ cho widget chạy nền: chỉ đọc RAM + dung lượng trong (có cache), bỏ qua /proc/meminfo.
     */
    static synchronized MemStats readLive(Context context, long maxStorageAgeMs) {
        MemStats s = new MemStats();
        readRam(context, s);
        long now = android.os.SystemClock.elapsedRealtime();
        if (cachedRomAt < 0 || now - cachedRomAt >= maxStorageAgeMs) {
            readStorage(context, s);
            cachedRomTotal = s.romTotal;
            cachedRomFree = s.romFree;
            cachedRomAt = now;
        } else {
            s.romTotal = cachedRomTotal;
            s.romFree = cachedRomFree;
            s.romUsed = Math.max(0L, s.romTotal - s.romFree);
            s.romPercent = percent(s.romUsed, s.romTotal);
        }
        return s;
    }

    private static void readRam(Context context, MemStats s) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        s.ramTotal = mi.totalMem;
        s.ramAvail = mi.availMem;
        s.lowMemory = mi.lowMemory;
        s.ramUsed = Math.max(0L, s.ramTotal - s.ramAvail);
        s.ramPercent = percent(s.ramUsed, s.ramTotal);
    }

    /** Đọc thêm chi tiết (cache, swap/zram) từ /proc/meminfo. Lỗi thì bỏ qua. */
    private static void readProcMeminfo(MemStats s) {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("Cached:")) {
                    s.ramCached = kb(line);
                } else if (line.startsWith("SwapTotal:")) {
                    s.swapTotal = kb(line);
                } else if (line.startsWith("SwapFree:")) {
                    s.swapFree = kb(line);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // không đọc được thì để 0
        }
    }

    private static long kb(String line) {
        String[] parts = line.trim().split("\\s+");
        return Long.parseLong(parts[1]) * 1024L;
    }

    private static void readStorage(Context context, MemStats s) {
        long total;
        long free;
        try {
            // Cho ra dung lượng "danh nghĩa" (vd 128 GB) và dung lượng trống giống app Cài đặt.
            StorageStatsManager ssm =
                    (StorageStatsManager) context.getSystemService(Context.STORAGE_STATS_SERVICE);
            total = ssm.getTotalBytes(StorageManager.UUID_DEFAULT);
            free = ssm.getFreeBytes(StorageManager.UUID_DEFAULT);
        } catch (IOException | RuntimeException e) {
            StatFs st = new StatFs(Environment.getDataDirectory().getPath());
            total = st.getTotalBytes();
            free = st.getAvailableBytes();
        }
        s.romTotal = total;
        s.romFree = free;
        s.romUsed = Math.max(0L, total - free);
        s.romPercent = percent(s.romUsed, total);
    }

    static int percent(long used, long total) {
        if (total <= 0) return 0;
        int p = Math.round(used * 100f / total);
        return Math.max(0, Math.min(100, p));
    }
}
