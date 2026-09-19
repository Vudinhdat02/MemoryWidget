package com.vdd.memwidget;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

/** Màn hình chi tiết RAM / ROM + dọn RAM + ghim widget. */
public class MainActivity extends Activity {
    private static final long REFRESH_MS = 2000L;

    private RingView ramRing;
    private RingView romRing;
    private TextView ramMain;
    private TextView ramSub;
    private TextView romMain;
    private TextView romSub;
    private LinearLayout ramDetails;
    private LinearLayout romDetails;
    private Button cleanButton;

    // Thư viện widget có xem trước trực tiếp
    private LinearLayout gallery;
    private final FrameLayout[] previewHosts = new FrameLayout[WidgetKinds.ALL.length];
    private final View[] previewViews = new View[WidgetKinds.ALL.length];
    private final String[] previewKeys = new String[WidgetKinds.ALL.length];
    private final float[] previewW = new float[WidgetKinds.ALL.length];
    private final float[] previewH = new float[WidgetKinds.ALL.length];

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            bind();
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.root);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });

        ramRing = findViewById(R.id.ram_ring);
        romRing = findViewById(R.id.rom_ring);
        ramMain = findViewById(R.id.ram_main);
        ramSub = findViewById(R.id.ram_sub);
        romMain = findViewById(R.id.rom_main);
        romSub = findViewById(R.id.rom_sub);
        ramDetails = findViewById(R.id.ram_details);
        romDetails = findViewById(R.id.rom_details);
        cleanButton = findViewById(R.id.btn_clean_ram);

        int track = getColor(R.color.track);
        int text = getColor(R.color.text_primary);
        ramRing.setColors(track, getColor(R.color.ring_ram), text);
        romRing.setColors(track, getColor(R.color.ring_rom), text);

        cleanButton.setOnClickListener(v -> cleanRam());
        findViewById(R.id.btn_storage).setOnClickListener(v -> openStorageSettings());
        gallery = findViewById(R.id.widget_gallery);
        buildGallery();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        WidgetUpdater.updateAll(this);
        MonitorService.start(this); // app đang hiện -> được phép bật service cập nhật widget
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void bind() {
        MemStats s = MemStats.read(this);

        ramRing.setPercent(s.ramPercent);
        ramMain.setText(Fmt.size(s.ramUsed, true) + " / " + Fmt.size(s.ramTotal, true));
        ramSub.setText(getString(R.string.sub_free, Fmt.size(s.ramAvail, true)));
        ramDetails.removeAllViews();
        addRow(ramDetails, R.string.row_used, Fmt.size(s.ramUsed, true));
        addRow(ramDetails, R.string.row_free, Fmt.size(s.ramAvail, true));
        if (s.ramCached > 0) addRow(ramDetails, R.string.row_cached, Fmt.size(s.ramCached, true));
        if (s.swapTotal > 0) {
            addRow(ramDetails, R.string.row_swap,
                    Fmt.size(s.swapTotal - s.swapFree, true) + " / " + Fmt.size(s.swapTotal, true));
        }

        romRing.setPercent(s.romPercent);
        romMain.setText(Fmt.size(s.romUsed, false) + " / " + Fmt.size(s.romTotal, false));
        romSub.setText(getString(R.string.sub_free, Fmt.size(s.romFree, false)));
        romDetails.removeAllViews();
        addRow(romDetails, R.string.row_used, Fmt.size(s.romUsed, false));
        addRow(romDetails, R.string.row_free, Fmt.size(s.romFree, false));
        addRow(romDetails, R.string.row_total, Fmt.size(s.romTotal, false));

        refreshGallery();
    }

    private void addRow(LinearLayout parent, int labelRes, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(6);
        row.setPadding(0, pad, 0, pad);

        TextView label = new TextView(this);
        label.setText(labelRes);
        label.setTextColor(getColor(R.color.text_secondary));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new TextView(this);
        val.setText(value);
        val.setTextColor(getColor(R.color.text_primary));
        val.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        val.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        row.addView(val, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        parent.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void cleanRam() {
        cleanButton.setEnabled(false);
        cleanButton.setText(R.string.cleaning);
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final MemCleaner.Result r = MemCleaner.clean(app);
            WidgetUpdater.updateAll(app);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                cleanButton.setEnabled(true);
                cleanButton.setText(R.string.btn_clean);
                Toast.makeText(this, WidgetActionReceiver.cleanMessage(app, r), Toast.LENGTH_SHORT).show();
                bind();
            });
        }).start();
    }

    private void openStorageSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            } catch (ActivityNotFoundException ignored) {
                // không mở được thì thôi
            }
        }
    }

    // ------------------------------------------------------------------ thư viện widget

    /** Dựng danh sách mọi widget: tên, cỡ ô, ảnh xem trước trực tiếp (đúng như trên màn hình chính) và nút thêm. */
    private void buildGallery() {
        float avail = getResources().getConfiguration().screenWidthDp - 40f - 28f;
        gallery.removeAllViews();
        for (int i = 0; i < WidgetKinds.ALL.length; i++) {
            final int index = i;
            WidgetKinds.Kind kind = WidgetKinds.ALL[i];

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(dp(14), dp(14), dp(14), dp(14));

            TextView title = new TextView(this);
            title.setText(kind.titleRes);
            title.setTextColor(getColor(R.color.text_primary));
            title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            card.addView(title);

            TextView size = new TextView(this);
            size.setText(getString(R.string.gallery_size, kind.cellsW, kind.cellsH));
            size.setTextColor(getColor(R.color.text_secondary));
            size.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            card.addView(size);

            float pw = Math.min(kind.prevW, avail);
            float ph = kind.prevH * pw / kind.prevW;
            previewW[i] = pw;
            previewH[i] = ph;

            FrameLayout stage = new FrameLayout(this);
            stage.setBackgroundResource(R.drawable.bg_stage);
            stage.setPadding(dp(12), dp(12), dp(12), dp(12));
            FrameLayout host = new FrameLayout(this);
            stage.addView(host, new FrameLayout.LayoutParams(dp(Math.round(pw)), dp(Math.round(ph)), Gravity.CENTER));
            previewHosts[i] = host;
            LinearLayout.LayoutParams stageLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            stageLp.topMargin = dp(10);
            card.addView(stage, stageLp);

            Button add = new Button(this);
            add.setText(R.string.gallery_add);
            add.setAllCaps(false);
            add.setTextColor(getColor(R.color.tonal_fg));
            add.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            add.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
            add.setBackgroundResource(R.drawable.bg_btn_tonal);
            add.setStateListAnimator(null);
            add.setOnClickListener(v -> pinWidget(index));
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
            addLp.topMargin = dp(12);
            card.addView(add, addLp);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(12);
            gallery.addView(card, cardLp);
        }
    }

    /** Cập nhật ảnh xem trước theo số liệu thật (bỏ qua widget nào không đổi). */
    private void refreshGallery() {
        if (gallery == null) return;
        Snapshot snap = Snapshot.read(this, 10_000L);
        for (int i = 0; i < WidgetKinds.ALL.length; i++) {
            FrameLayout host = previewHosts[i];
            if (host == null) continue;
            String key = WidgetKinds.key(i, snap);
            if (previewViews[i] != null && key.equals(previewKeys[i])) continue;
            try {
                RemoteViews rv = WidgetKinds.build(this, i, snap, previewW[i], previewH[i], true);
                View v = previewViews[i];
                if (v == null) {
                    v = rv.apply(this, host);
                    host.addView(v, new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    previewViews[i] = v;
                } else {
                    rv.reapply(this, v);
                }
                previewKeys[i] = key;
            } catch (RuntimeException ignored) {
                // Xem trước lỗi thì bỏ qua, không được làm sập màn hình chính của app.
            }
        }
    }

    private void pinWidget(int kindIndex) {
        AppWidgetManager mgr = getSystemService(AppWidgetManager.class);
        if (mgr != null && mgr.isRequestPinAppWidgetSupported()) {
            Bundle extras = null;
            try {
                RemoteViews preview = WidgetKinds.build(this, kindIndex, Snapshot.read(this, 10_000L),
                        previewW[kindIndex], previewH[kindIndex], true);
                extras = new Bundle();
                extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, preview);
            } catch (RuntimeException e) {
                extras = null;
            }
            mgr.requestPinAppWidget(new ComponentName(this, WidgetKinds.ALL[kindIndex].provider), extras, null);
        } else {
            Toast.makeText(this, R.string.pin_unsupported, Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics()));
    }
}
