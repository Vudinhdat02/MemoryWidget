package com.vdd.memwidget;

import java.util.Locale;

/** Định dạng dung lượng cho dễ đọc. */
final class Fmt {
    private Fmt() {}

    /**
     * @param binary true = 1024 (dùng cho RAM), false = 1000 (dùng cho ROM, giống màn hình Cài đặt của Android)
     */
    static String size(long bytes, boolean binary) {
        double unit = binary ? 1024d : 1000d;
        double mb = bytes / (unit * unit);
        double gb = mb / unit;
        Locale l = Locale.getDefault();
        if (gb >= 100) return String.format(l, "%.0f GB", gb);
        if (gb >= 1) return String.format(l, "%.1f GB", gb);
        return String.format(l, "%.0f MB", mb);
    }

    /** Số GB (không kèm đơn vị) với đúng {@code decimals} chữ số thập phân, ví dụ "4.24". */
    static String gbFixed(long bytes, boolean binary, int decimals) {
        double unit = binary ? 1024d : 1000d;
        double gb = bytes / (unit * unit * unit);
        return String.format(Locale.getDefault(), "%." + decimals + "f", gb);
    }

    /** Số GB gọn: tối đa {@code maxDecimals} chữ số thập phân, bỏ số 0 thừa ("225", "120.5"). */
    static String gbTrim(long bytes, boolean binary, int maxDecimals) {
        String s = gbFixed(bytes, binary, maxDecimals);
        char sep = java.text.DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator();
        if (s.indexOf(sep) < 0) return s;
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == sep) end--;
        return s.substring(0, end);
    }
}
