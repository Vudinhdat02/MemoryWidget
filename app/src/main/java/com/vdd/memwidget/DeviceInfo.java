package com.vdd.memwidget;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Thông tin cố định của máy: tên, phiên bản Android, chip. Tính một lần rồi nhớ lại. */
final class DeviceInfo {
    private static String name;
    private static String full;
    private static String android;
    private static String soc;

    private static final Map<String, String> SOC_NAMES = new HashMap<>();
    static {
        // Qualcomm Snapdragon
        SOC_NAMES.put("SM8850", "Snapdragon 8 Elite Gen 5");
        SOC_NAMES.put("SM8750", "Snapdragon 8 Elite");
        SOC_NAMES.put("SM8650", "Snapdragon 8 Gen 3");
        SOC_NAMES.put("SM8550", "Snapdragon 8 Gen 2");
        SOC_NAMES.put("SM8475", "Snapdragon 8+ Gen 1");
        SOC_NAMES.put("SM8450", "Snapdragon 8 Gen 1");
        SOC_NAMES.put("SM8350", "Snapdragon 888");
        SOC_NAMES.put("SM8250", "Snapdragon 865");
        SOC_NAMES.put("SM8150", "Snapdragon 855");
        SOC_NAMES.put("SM7675", "Snapdragon 7+ Gen 3");
        SOC_NAMES.put("SM7550", "Snapdragon 7 Gen 3");
        // Samsung Exynos
        SOC_NAMES.put("S5E9955", "Exynos 2500");
        SOC_NAMES.put("S5E9945", "Exynos 2400");
        SOC_NAMES.put("S5E9925", "Exynos 2200");
        // MediaTek Dimensity
        SOC_NAMES.put("MT6991", "Dimensity 9400");
        SOC_NAMES.put("MT6989", "Dimensity 9300");
        SOC_NAMES.put("MT6985", "Dimensity 9200");
        SOC_NAMES.put("MT6983", "Dimensity 9000");
        // Google Tensor
        SOC_NAMES.put("GS101", "Tensor G1");
        SOC_NAMES.put("GS201", "Tensor G2");
        SOC_NAMES.put("GS301", "Tensor G3");
    }

    private DeviceInfo() {}

    /** Tên máy dạng ngắn, ví dụ "Galaxy S23" (lấy từ tên thiết bị trong Cài đặt, không có thì dùng mã model). */
    static synchronized String name(Context context) {
        if (name == null) {
            String n = null;
            try {
                n = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
            } catch (RuntimeException ignored) {
                // dùng model
            }
            if (n == null || n.trim().isEmpty()) n = Build.MODEL;
            name = n.trim();
        }
        return name;
    }

    /** Ví dụ "SAMSUNG Galaxy S23". */
    static synchronized String fullName(Context context) {
        if (full == null) {
            String n = name(context);
            String maker = Build.MANUFACTURER == null ? "" : Build.MANUFACTURER.trim();
            if (maker.isEmpty() || n.toLowerCase(Locale.ROOT).startsWith(maker.toLowerCase(Locale.ROOT))) {
                full = n;
            } else {
                full = maker.toUpperCase(Locale.ROOT) + " " + n;
            }
        }
        return full;
    }

    /** Ví dụ "Android 16 Baklava (API 36)". */
    static synchronized String androidLine() {
        if (android == null) {
            int api = Build.VERSION.SDK_INT;
            String dessert = dessert(api);
            android = "Android " + Build.VERSION.RELEASE
                    + (dessert.isEmpty() ? "" : " " + dessert) + " (API " + api + ")";
        }
        return android;
    }

    private static String dessert(int api) {
        switch (api) {
            case 31:
            case 32: return "Snow Cone";
            case 33: return "Tiramisu";
            case 34: return "Upside Down Cake";
            case 35: return "Vanilla Ice Cream";
            case 36: return "Baklava";
            default: return "";
        }
    }

    /** Ví dụ "Qualcomm® Snapdragon 8 Gen 2". Chip lạ thì hiện mã của hãng. */
    static synchronized String socLine() {
        if (soc == null) {
            String manu = clean(Build.SOC_MANUFACTURER);
            String model = clean(Build.SOC_MODEL);
            if (model.isEmpty()) model = clean(Build.HARDWARE);

            String key = model.toUpperCase(Locale.ROOT);
            int dash = key.indexOf('-');
            if (dash > 0) key = key.substring(0, dash);
            String marketing = SOC_NAMES.get(key);

            String brand;
            String m = manu.toLowerCase(Locale.ROOT);
            if (m.equals("qti") || m.contains("qualcomm")) brand = "Qualcomm®";
            else if (m.contains("samsung") || m.equals("slsi") || m.equals("s.lsi")) brand = "Samsung";
            else if (m.contains("mediatek")) brand = "MediaTek";
            else if (m.contains("google")) brand = "Google";
            else brand = manu;

            String chip = marketing != null ? marketing : model;
            if (marketing != null && marketing.startsWith("Tensor") && brand.equals("Google")) {
                soc = "Google " + marketing;
            } else {
                soc = (brand.isEmpty() ? chip : brand + " " + chip).trim();
            }
            if (soc.isEmpty()) soc = "?";
        }
        return soc;
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.equalsIgnoreCase(Build.UNKNOWN) ? "" : s;
    }
}
