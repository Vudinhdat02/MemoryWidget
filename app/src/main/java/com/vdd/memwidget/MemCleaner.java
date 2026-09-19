package com.vdd.memwidget;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dọn RAM: dừng tiến trình nền của các ứng dụng và xóa cache của chính app này.
 * (Android không cho app thường xóa cache của app khác; đây là cách hợp lệ không cần root.)
 */
final class MemCleaner {
    private MemCleaner() {}

    static final class Result {
        long freedBytes;
        long ownCacheBytes;
    }

    /** Gọi từ luồng nền (có sleep ngắn để chờ hệ thống thu hồi RAM). */
    @SuppressWarnings("deprecation")
    static Result clean(Context context) {
        Context app = context.getApplicationContext();
        Result result = new Result();

        ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo before = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(before);

        PackageManager pm = app.getPackageManager();
        String self = app.getPackageName();
        String home = defaultHomePackage(pm);

        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        Set<String> seen = new HashSet<>();
        for (ResolveInfo ri : apps) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (pkg == null || pkg.equals(self) || pkg.equals(home) || !seen.add(pkg)) continue;
            try {
                am.killBackgroundProcesses(pkg);
            } catch (RuntimeException ignored) {
                // bỏ qua app không dừng được
            }
        }

        result.ownCacheBytes = clearOwnCache(app);

        SystemClock.sleep(900); // chờ hệ thống thu hồi bộ nhớ
        ActivityManager.MemoryInfo after = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(after);
        result.freedBytes = Math.max(0L, after.availMem - before.availMem);
        return result;
    }

    @SuppressWarnings("deprecation")
    private static String defaultHomePackage(PackageManager pm) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) return ri.activityInfo.packageName;
        } catch (RuntimeException ignored) {
            // không xác định được launcher thì thôi
        }
        return "";
    }

    private static long clearOwnCache(Context context) {
        long freed = deleteContents(context.getCacheDir());
        File ext = context.getExternalCacheDir();
        if (ext != null) freed += deleteContents(ext);
        return freed;
    }

    private static long deleteContents(File dir) {
        long freed = 0;
        File[] children = dir == null ? null : dir.listFiles();
        if (children == null) return 0;
        for (File f : children) {
            if (f.isDirectory()) freed += deleteContents(f);
            long len = f.isFile() ? f.length() : 0;
            if (f.delete()) freed += len;
        }
        return freed;
    }
}
