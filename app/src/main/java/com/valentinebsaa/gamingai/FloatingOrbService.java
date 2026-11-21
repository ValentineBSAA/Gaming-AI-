package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Explicitly enabled, draggable, tiny Android system overlay.
 * No game capture, microphone monitoring, network traffic or automatic interaction.
 */
public final class FloatingOrbService extends Service {
    public static final String ACTION_SHOW = "com.valentinebsaa.gamingai.SHOW";
    public static final String ACTION_UPDATE = "com.valentinebsaa.gamingai.UPDATE";
    public static final String ACTION_STOP = "com.valentinebsaa.gamingai.STOP";
    public static final String ACTION_THEME = "com.valentinebsaa.gamingai.THEME";
    public static final String EXTRA_STATUS = "orb_status";
    private static final String CHANNEL_ID = "gamingai_orb";
    private static final int NOTIFICATION_ID = 42;
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private TextView label;
    private View indicator;
    private final ImageView[] orbLayers = new ImageView[3];
    private String currentState = "IDLE";
    private float startX;
    private float startY;
    private int originX;
    private int originY;
    private boolean dragged;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Keep orb reachable if firmware changes the display bounds or orientation. */
    @Override public void onConfigurationChanged(Configuration updated) {
        super.onConfigurationChanged(updated);
        if (panel == null || params == null || windows == null) return;
        DisplayMetrics dims = new DisplayMetrics();
        windows.getDefaultDisplay().getMetrics(dims);
        int clampedX = Math.max(0, Math.min(Math.max(0,
            dims.widthPixels - dp(94)), params.x));
        int clampedY = Math.max(0, Math.min(Math.max(0,
            dims.heightPixels - dp(103)), params.y));
        if (params.x != clampedX || params.y != clampedY) {
            params.x = clampedX;
            params.y = clampedY;
            try {
                windows.updateViewLayout(panel, params);
                getSharedPreferences("orb_position", MODE_PRIVATE).edit()
                    .putInt("x", params.x).putInt("y", params.y).apply();
            } catch (IllegalArgumentException windowGone) {
                // Surface may have been dismissed during an OS configuration change.
            }
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            getSharedPreferences("permission_locator_only", MODE_PRIVATE).edit()
                .putBoolean("overlay_enabled", false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        ensureForegroundNotification();
        if (panel == null) createOrb();
        if (intent != null && intent.hasExtra(EXTRA_STATUS)) {
            currentState = OrbStatus.normalize(intent.getStringExtra(EXTRA_STATUS));
        }
        updateStatus(currentState);
        return START_NOT_STICKY;
    }

    private void ensureForegroundNotification() {
        NotificationManager manager = (NotificationManager)
            getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Gaming AI floating orb", NotificationManager.IMPORTANCE_LOW));
        }
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent launch = PendingIntent.getActivity(this, 1, open,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, FloatingOrbService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent close = PendingIntent.getService(this, 2, stop,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);
        Notification notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Gaming AI orb is visible")
            .setContentText("Drag the orb or tap it to manage")
            .setContentIntent(launch)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                "Hide orb", close)
            .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private FrameLayout.LayoutParams centeredImageParams() {
        return new FrameLayout.LayoutParams(dp(76), dp(76), Gravity.CENTER);
    }

    private ImageView addLayer(FrameLayout stack, int resourceId) {
        ImageView image = new ImageView(this);
        image.setImageResource(resourceId);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        stack.addView(image, centeredImageParams());
        return image;
    }

    private void createOrb() {
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setContentDescription("Gaming AI companion overlay. Drag to move, tap to open.");

        FrameLayout orb = new FrameLayout(this);
        panel.addView(orb, new LinearLayout.LayoutParams(dp(80), dp(80)));
        orbLayers[0] = addLayer(orb, R.drawable.orb_core);
        orbLayers[1] = addLayer(orb, R.drawable.orb_petals);
        orbLayers[2] = addLayer(orb, R.drawable.orb_ring);

        indicator = new View(this);
        FrameLayout.LayoutParams dot = new FrameLayout.LayoutParams(dp(13), dp(13),
            Gravity.BOTTOM | Gravity.RIGHT);
        dot.rightMargin = dp(6);
        dot.bottomMargin = dp(6);
        orb.addView(indicator, dot);

        label = new TextView(this);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(Color.WHITE);
        label.setShadowLayer(dp(2), 0, dp(1), Color.BLACK);
        panel.addView(label, new LinearLayout.LayoutParams(dp(90), dp(20)));

        params = new WindowManager.LayoutParams(dp(94), dp(103),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        DisplayMetrics display = new DisplayMetrics();
        windows.getDefaultDisplay().getMetrics(display);
        params.x = Math.max(0, Math.min(display.widthPixels - dp(94),
            getSharedPreferences("orb_position", MODE_PRIVATE)
                .getInt("x", display.widthPixels - dp(108))));
        params.y = Math.max(0, Math.min(display.heightPixels - dp(103),
            getSharedPreferences("orb_position", MODE_PRIVATE)
                .getInt("y", dp(100))));

        panel.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = event.getRawX();
                    startY = event.getRawY();
                    originX = params.x;
                    originY = params.y;
                    dragged = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - startX;
                    float deltaY = event.getRawY() - startY;
                    if (Math.abs(deltaX) > dp(7) || Math.abs(deltaY) > dp(7)) dragged = true;
                    if (dragged) {
                        DisplayMetrics dims = new DisplayMetrics();
                        windows.getDefaultDisplay().getMetrics(dims);
                        params.x = Math.max(0, Math.min(Math.max(0, dims.widthPixels - dp(94)),
                            originX + Math.round(deltaX)));
                        params.y = Math.max(0, Math.min(Math.max(0, dims.heightPixels - dp(103)),
                            originY + Math.round(deltaY)));
                        windows.updateViewLayout(panel, params);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (dragged) {
                        getSharedPreferences("orb_position", MODE_PRIVATE).edit()
                            .putInt("x", params.x).putInt("y", params.y).apply();
                    } else {
                        Intent openApp = new Intent(this, MainActivity.class);
                        openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(openApp);
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default: return false;
            }
        });

        windows.addView(panel, params);
    }

    private void updateStatus(String state) {
        if (label == null || indicator == null) return;
        currentState = state;
        int chosen = getSharedPreferences(OrbPalette.PREFS, MODE_PRIVATE)
            .getInt(OrbPalette.COLOR_KEY, OrbPalette.DEFAULT_COLOR);
        boolean automatic = getSharedPreferences(OrbPalette.PREFS, MODE_PRIVATE)
            .getBoolean(OrbPalette.AUTO_KEY, false);
        int tint = OrbPalette.resolve(automatic, chosen, state);
        for (ImageView layer : orbLayers) {
            if (layer != null) layer.setColorFilter(tint, PorterDuff.Mode.MULTIPLY);
        }
        label.setText(state);
        int color;
        if ("READY".equals(state)) color = Color.rgb(134, 233, 196);
        else if ("SAVED".equals(state)) color = Color.rgb(174, 231, 255);
        else if ("NO SD".equals(state)) color = Color.rgb(255, 175, 90);
        else if ("ERROR".equals(state)) color = Color.rgb(255, 102, 123);
        else color = Color.rgb(204, 169, 255);
        GradientDrawable lamp = new GradientDrawable();
        lamp.setShape(GradientDrawable.OVAL);
        lamp.setColor(color);
        lamp.setStroke(dp(1), Color.WHITE);
        indicator.setBackground(lamp);
    }

    @Override public void onDestroy() {
        if (panel != null && windows != null) {
            try { windows.removeView(panel); } catch (Exception ignored) {}
            panel = null;
        }
        stopForeground(true);
        super.onDestroy();
    }
}
