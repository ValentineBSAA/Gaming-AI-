package com.valentinebsaa.gamingai;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Lightweight native touch controls; no animation loops while playing a game. */
public final class TouchUi {
    public static final String PREFS = "touch_ui_preferences";
    public static final String REDUCED_MOTION = "reduce_motion";
    private TouchUi() {}

    public static int dp(Activity activity, float amount) {
        return Math.round(activity.getResources().getDisplayMetrics().density * amount);
    }

    public static int widthDp(Activity activity) {
        int configurationWidth = activity.getResources().getConfiguration().screenWidthDp;
        return configurationWidth > 0 ? configurationWidth : Math.round(
            activity.getResources().getDisplayMetrics().widthPixels /
            activity.getResources().getDisplayMetrics().density);
    }

    public static boolean reduceMotion(Activity activity) {
        return activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
            .getBoolean(REDUCED_MOTION, false) || !ValueAnimator.areAnimatorsEnabled();
    }

    public static ScrollView page(Activity activity) {
        ScrollView s = new ScrollView(activity);
        s.setFillViewport(true);
        s.setBackgroundColor(Color.rgb(15, 21, 35));
        s.setVerticalScrollBarEnabled(true);
        s.setScrollbarFadingEnabled(true);
        s.setVerticalFadingEdgeEnabled(true);
        s.setFadingEdgeLength(dp(activity, 12));
        s.setSmoothScrollingEnabled(true);
        s.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        s.setFocusableInTouchMode(true);
        return s;
    }

    public static LinearLayout root(Activity activity, ScrollView scroll) {
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, SquareLayoutPolicy.paddingDp(widthDp(activity)));
        root.setPadding(padding, padding, padding, padding + dp(activity, 14));
        root.setClipToPadding(false);
        scroll.addView(root);
        return root;
    }

    public static LinearLayout card(Activity activity, LinearLayout parent) {
        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(activity, SquareLayoutPolicy.cardPaddingDp(widthDp(activity)));
        card.setPadding(padding, padding, padding, padding);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(30, 39, 57));
        background.setCornerRadius(dp(activity, 17));
        background.setStroke(dp(activity, 1), Color.rgb(65, 75, 95));
        card.setBackground(background);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(activity, 12);
        parent.addView(card, p);
        return card;
    }

    public static HorizontalScrollView rail(Activity activity, LinearLayout parent) {
        HorizontalScrollView rail = new HorizontalScrollView(activity);
        rail.setHorizontalScrollBarEnabled(false);
        rail.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        parent.addView(rail, new LinearLayout.LayoutParams(-1, -2));
        return rail;
    }

    public static void styleButton(Activity activity, Button button) {
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(242, 236, 244));
        button.setTextSize(14);
        button.setMinHeight(dp(activity, 52));
        button.setMinimumWidth(dp(activity, 48));
        button.setPadding(dp(activity, 12), dp(activity, 9),
            dp(activity, 12), dp(activity, 9));
        button.setSingleLine(false);
        button.setMaxLines(3);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(Color.rgb(83, 49, 97));
        shape.setCornerRadius(dp(activity, 14));
        shape.setStroke(dp(activity, 1), Color.rgb(132, 94, 142));
        button.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x55FFFFFF), shape, null));
        button.setOnTouchListener((view, event) -> {
            if (reduceMotion(activity)) return false;
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                view.animate().cancel();
                view.animate().scaleX(.975f).scaleY(.975f).setDuration(95).start();
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.animate().cancel();
                view.animate().scaleX(1f).scaleY(1f).setDuration(135).start();
            }
            return false; // Keep Android click, keyboard and accessibility behavior.
        });
    }

    public static Button button(Activity activity, LinearLayout parent,
            String label, View.OnClickListener onClick) {
        Button b = new Button(activity);
        b.setText(label);
        styleButton(activity, b);
        b.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            onClick.onClick(view);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(activity, 6);
        parent.addView(b, params);
        return b;
    }

    public static Button chip(Activity activity, LinearLayout row,
            String name, View.OnClickListener onClick) {
        Button b = new Button(activity);
        b.setText(name);
        styleButton(activity, b);
        b.setTextSize(12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.rightMargin = dp(activity, 7);
        row.addView(b, params);
        b.setOnClickListener(onClick);
        return b;
    }

    public static void jumpTo(Activity activity, ScrollView scroll, View target) {
        if (target == null || scroll == null) return;
        scroll.post(() -> {
            int y = Math.max(0, target.getTop() - dp(activity, 8));
            if (reduceMotion(activity)) scroll.scrollTo(0, y);
            else scroll.smoothScrollTo(0, y);
        });
    }

    public static void reveal(Activity activity, View view) {
        if (reduceMotion(activity)) return;
        view.setAlpha(0f);
        view.setTranslationY(dp(activity, 8));
        view.animate().alpha(1f).translationY(0f).setDuration(210).start();
    }
}
