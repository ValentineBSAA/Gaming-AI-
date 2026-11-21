package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ScrollView;

/**
 * Controller shortcuts apply ONLY while a Gaming AI Activity holds focus.
 * A non-focusable orb overlay cannot intercept PSP input in another app.
 */
public abstract class HandheldActivity extends Activity {
    private ScrollView activePage;

    final void registerHandheldPage(ScrollView page) {
        activePage = page;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_BUTTON_B:
                    onBackPressed();
                    return true;
                case KeyEvent.KEYCODE_BUTTON_R1:
                    if (activePage != null) {
                        activePage.pageScroll(View.FOCUS_DOWN);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_BUTTON_L1:
                    if (activePage != null) {
                        activePage.pageScroll(View.FOCUS_UP);
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
