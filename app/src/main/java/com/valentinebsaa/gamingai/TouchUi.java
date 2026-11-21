package com.valentinebsaa.gamingai;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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
    public static final int INK = 0xfff6f2ff;
    public static final int LAVENDER = 0xffc9abf7;
    public static final int TEAL = 0xff83eee2;
    public static final int MUTED = 0xffb3bed1;
    public static final int GLASS_EDGE = 0xff536081;
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
        s.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xff101a30, 0xff0b1020}));
        activity.getWindow().setStatusBarColor(0xff0d1426);
        activity.getWindow().setNavigationBarColor(0xff0b1020);
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
        GradientDrawable background = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff28334d, 0xff19253c, 0xff172136});
        background.setCornerRadius(dp(activity, 20));
        background.setStroke(dp(activity, 1), 0xff4e6281);
        card.setBackground(background);
        card.setElevation(dp(activity, 3));
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
        button.setTextColor(INK);
        button.setTextSize(14);
        button.setMinHeight(dp(activity, 52));
        button.setMinimumWidth(dp(activity, 48));
        button.setPadding(dp(activity, 12), dp(activity, 9),
            dp(activity, 12), dp(activity, 9));
        button.setSingleLine(false);
        button.setMaxLines(3);
        GradientDrawable shape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff704c87, 0xff4c416d});
        shape.setCornerRadius(dp(activity, 14));
        shape.setStroke(dp(activity, 1), 0xff9879a8);
        button.setElevation(dp(activity, 1));
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
        b.setTextColor(TEAL);
        GradientDrawable chipShape = new GradientDrawable();
        chipShape.setColor(0xff202f46);
        chipShape.setCornerRadius(dp(activity, 14));
        chipShape.setStroke(dp(activity, 1), 0xff50687e);
        b.setBackground(new RippleDrawable(
            ColorStateList.valueOf(0x557be8db), chipShape, null));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.rightMargin = dp(activity, 7);
        row.addView(b, params);
        b.setOnClickListener(onClick);
        return b;
    }

    /** Shared header styling. Reuses the real transparent orb PNG layers. */
    public static void header(Activity activity, LinearLayout parent,
            String title, String description) {
        LinearLayout hero = new LinearLayout(activity);
        hero.setGravity(android.view.Gravity.CENTER_VERTICAL);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        int pad = dp(activity, 14);
        hero.setPadding(pad, pad, pad, pad);
        GradientDrawable bg = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            new int[]{0xff34415f, 0xff242c47, 0xff1e2942});
        bg.setCornerRadius(dp(activity, 23));
        bg.setStroke(dp(activity, 1), 0xff7680a7);
        hero.setBackground(bg);
        hero.setElevation(dp(activity, 5));
        LinearLayout.LayoutParams frame = new LinearLayout.LayoutParams(-1, -2);
        frame.bottomMargin = dp(activity, 13);
        parent.addView(hero, frame);

        FrameLayout orb = new FrameLayout(activity);
        int orbSize = dp(activity, widthDp(activity) < 320 ? 52 : 64);
        hero.addView(orb, new LinearLayout.LayoutParams(orbSize, orbSize));
        int[] images = {R.drawable.orb_core, R.drawable.orb_petals, R.drawable.orb_ring};
        for (int resource : images) {
            ImageView layer = new ImageView(activity);
            layer.setImageResource(resource);
            layer.setScaleType(ImageView.ScaleType.FIT_CENTER);
            layer.setColorFilter(0xffc4b3fd, PorterDuff.Mode.MULTIPLY);
            orb.addView(layer, new FrameLayout.LayoutParams(-1, -1));
        }

        LinearLayout headings = new LinearLayout(activity);
        headings.setOrientation(LinearLayout.VERTICAL);
        headings.setPadding(dp(activity, 11), 0, 0, 0);
        hero.addView(headings, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView name = new TextView(activity);
        name.setText(title);
        name.setTextColor(INK);
        name.setTextSize(widthDp(activity) < 320 ? 19 : 23);
        name.setTypeface(android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD);
        name.setSingleLine(false);
        headings.addView(name);

        TextView desc = new TextView(activity);
        desc.setText(description);
        desc.setTextColor(TEAL);
        desc.setTextSize(11);
        desc.setLetterSpacing(.055f);
        desc.setSingleLine(false);
        desc.setPadding(0, dp(activity, 3), 0, 0);
        headings.addView(desc);
    }

    /** Consistent, restrained section typography across all screens. */
    public static void decorateLabel(TextView label, String text, int size) {
        if (label == null || text == null) return;
        if (size >= 14 && text.length() <= 36 && text.equals(text.toUpperCase(java.util.Locale.ROOT))) {
            label.setTextColor(LAVENDER);
            label.setTypeface(android.graphics.Typeface.DEFAULT,
                android.graphics.Typeface.BOLD);
            label.setLetterSpacing(.06f);
        }
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
