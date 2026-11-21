package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Locale;

/**
 * Guided first-day verification, with honest PASS / NEEDS ACTION / NOT TESTED
 * indicators. Does not silently request mic, grant overlays or record gameplay.
 */
public final class SetupGuideActivity extends Activity {
    private static final String URI_PREFS = "permission_locator_only";
    private static final String KEY_URI = "sd_tree_uri";
    private static final String CHECKS_PREFS = "first_day_device_manual_checks";
    private static final int FOREGROUND = 0xfff6f2ff;
    private static final int MUTED = 0xffb3bed1;
    private TextView headline;
    private TextView screenDetails;
    private TextView sdDetails;
    private TextView overlayDetails;
    private TextView voiceDetails;
    private CheckBox touchPassed;
    private CheckBox swivelPassed;
    private CheckBox ppssppPassed;
    private CheckBox audioPassed;
    private boolean sdTestPassed = false;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView scroll = TouchUi.page(this);
        LinearLayout root = TouchUi.root(this, scroll);
        setContentView(scroll);
        TouchUi.header(this, root, "First-day Setup",
            "RG ROTATE • STEP-BY-STEP CHECK");
        headline = addText(root, "Nothing tested on the physical Rotate yet. "
            + "Follow each step after the device arrives.", 13, MUTED);

        LinearLayout device = TouchUi.card(this, root);
        heading(device, "1 • DISPLAY & TOUCH");
        screenDetails = addText(device, "", 13, MUTED);
        touchPassed = check(device, "I tested scrolling and tapping at 720 × 720", "touch");
        swivelPassed = check(device, "I tested opening and closing the swivel screen", "swivel");

        LinearLayout sd = TouchUi.card(this, root);
        heading(sd, "2 • REMOVABLE MICROSD");
        sdDetails = addText(sd, "", 13, MUTED);
        TouchUi.button(this, sd, "Choose / change microSD folder in Dashboard",
            v -> finish());
        TouchUi.button(this, sd, "Run microSD write / read / delete test",
            v -> testMicroSd());

        LinearLayout overlay = TouchUi.card(this, root);
        heading(overlay, "3 • COMPANION OVERLAY");
        overlayDetails = addText(overlay, "", 13, MUTED);
        TouchUi.button(this, overlay, "Open Android overlay permission",
            v -> requestOverlay());
        addText(overlay, "After granting permission, use Dashboard to show the orb. "
            + "Check that it moves without blocking game controls.", 12, MUTED);

        LinearLayout voices = TouchUi.card(this, root);
        heading(voices, "4 • FREE OFFLINE VOICE");
        voiceDetails = addText(voices, "", 13, MUTED);
        TouchUi.button(this, voices, "Open Voice Lab / offline voice setup",
            v -> {
                Intent intent = new Intent(this, VoiceLabActivity.class);
                intent.putExtra(VoiceLabActivity.EXTRA_GAME, "General");
                startActivity(intent);
            });
        audioPassed = check(voices,
            "I previewed a voice with Airplane Mode enabled", "audio");

        LinearLayout games = TouchUi.card(this, root);
        heading(games, "5 • PPSSPP & GAMEPLAY");
        ppssppPassed = check(games,
            "I tested PSP gameplay with orb hidden and visible", "ppsspp");
        addText(games, "Later: The 3rd Birthday, Midnight Club 3, "
            + "and Midnight Club: LA Remix. Observe battery, heat, frame rate, "
            + "Bluetooth sound, and whether the overlay steals taps. "
            + "No game capture or automatic game recognition is active.", 12, MUTED);

        LinearLayout report = TouchUi.card(this, root);
        heading(report, "6 • SHARE A SAFE DIAGNOSTIC");
        addText(report, "Copies display, Android, overlay and manually reported "
            + "test results. No private notes, file locations, microphones or screenshots.",
            12, MUTED);
        TouchUi.button(this, report, "Copy diagnostic report", v -> copyReport());
        TouchUi.button(this, report, "Refresh system checks", v -> refreshStatus());
        TouchUi.button(this, report, "Reset my manual checkmarks", v -> {
            new AlertDialog.Builder(this)
                .setTitle("Reset manual device test results?")
                .setMessage("This clears only your four manual checklist selections. "
                    + "Saved game memories and microSD files will not be touched.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset tests", (dialog, action) -> {
                    getSharedPreferences(CHECKS_PREFS, MODE_PRIVATE).edit().clear().apply();
                    touchPassed.setChecked(false);
                    swivelPassed.setChecked(false);
                    audioPassed.setChecked(false);
                    ppssppPassed.setChecked(false);
                    sdTestPassed = false;
                    refreshStatus();
                }).show();
        });
        TouchUi.button(this, root, "Return to Dashboard", v -> finish());
        refreshStatus();
        TouchUi.reveal(this, root);
    }

    private TextView addText(LinearLayout parent, String text, int size, int color) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setPadding(0, TouchUi.dp(this, 7), 0, TouchUi.dp(this, 7));
        parent.addView(label, new LinearLayout.LayoutParams(-1, -2));
        return label;
    }

    private void heading(LinearLayout card, String heading) {
        TextView title = addText(card, heading, 15, FOREGROUND);
        title.setTypeface(android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD);
        title.setTextColor(TouchUi.LAVENDER);
    }

    private CheckBox check(LinearLayout parent, String text, String key) {
        CheckBox box = new CheckBox(this);
        box.setText(text);
        box.setTextColor(FOREGROUND);
        box.setTextSize(13);
        box.setMinHeight(TouchUi.dp(this, 52));
        box.setSingleLine(false);
        box.setChecked(getSharedPreferences(CHECKS_PREFS, MODE_PRIVATE)
            .getBoolean(key, false));
        box.setOnCheckedChangeListener((control, checked) -> {
            getSharedPreferences(CHECKS_PREFS, MODE_PRIVATE).edit()
                .putBoolean(key, checked).apply();
            if (headline != null) updateSummary();
        });
        parent.addView(box, new LinearLayout.LayoutParams(-1, -2));
        return box;
    }

    private Uri selectedFolder() throws IOException {
        String raw = getSharedPreferences(URI_PREFS, MODE_PRIVATE)
            .getString(KEY_URI, null);
        if (raw == null) throw new IOException("No microSD folder chosen.");
        Uri folder = Uri.parse(raw);
        if (!StorageRules.isRemovableSdSubfolder(folder.getAuthority(),
                DocumentsContract.getTreeDocumentId(folder)))
            throw new IOException("Selected folder is not a verified removable microSD subfolder.");
        return folder;
    }

    private void refreshStatus() {
        android.util.DisplayMetrics display = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(display);
        int dpWidth = TouchUi.widthDp(this);
        String dimensions = display.widthPixels + " × " + display.heightPixels;
        screenDetails.setText("Detected: " + dimensions
            + " physical px • usable " + dpWidth + " dp • "
            + String.format(Locale.US, "%.2f", display.density) + " density."
            + "\nAndroid " + Build.VERSION.RELEASE + " • "
            + Build.MANUFACTURER + " " + Build.MODEL
            + "\nExpected Rotate panel: 720 × 720. "
            + "Touch and swivel need your manual confirmation.");
        try {
            Uri folder = selectedFolder();
            int memories = new MemoryStore(this, folder).load().size();
            int profiles = new GameProfileStore(this, folder).load().size();
            sdDetails.setText("READ PASS: " + memories + " saved notes, "
                + profiles + " custom profiles. "
                + (sdTestPassed ? "WRITE / READ / DELETE PASS." :
                    "Write/read/delete NOT TESTED in this session."));
        } catch (Exception missing) {
            sdTestPassed = false;
            sdDetails.setText("NEEDS ACTION: " + missing.getMessage()
                + "\nNo fallback to internal memory.");
        }
        overlayDetails.setText(Settings.canDrawOverlays(this)
            ? "Permission GRANTED. Actual in-game overlay NOT TESTED."
            : "Permission OFF. Tap the button to open Android settings.");
        String asrStatus;
        try {
            asrStatus = Build.VERSION.SDK_INT >= 31
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
                ? "On-device recognizer advertised. Actual microphone test NOT RUN."
                : "No on-device recognizer advertised. Typed notes still work.";
        } catch (Exception error) {
            asrStatus = "Cannot inspect speech recognizer; use text fallback.";
        }
        voiceDetails.setText("Installed TTS voices must be auditioned in Voice Lab. "
            + asrStatus + " No paid API is connected.");
        updateSummary();
    }

    private void updateSummary() {
        if (headline == null || touchPassed == null || swivelPassed == null
                || ppssppPassed == null || audioPassed == null) return;
        int count = (touchPassed.isChecked() ? 1 : 0)
            + (swivelPassed.isChecked() ? 1 : 0)
            + (audioPassed.isChecked() ? 1 : 0)
            + (ppssppPassed.isChecked() ? 1 : 0);
        headline.setText(count + " of 4 manual device checks reported complete. "
            + "These are your own checkmarks, not automated proof. "
            + "System tests are shown separately below.");
    }

    private String appVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0)
                .versionName;
        } catch (Exception unavailable) {
            return "unknown";
        }
    }

    private void testMicroSd() {
        try {
            Uri folder = selectedFolder();
            new MemoryStore(this, folder).selfTest();
            sdTestPassed = true;
            refreshStatus();
            headline.setText("PASS: microSD temporary file was written, "
                + "read back, and deleted.");
        } catch (Exception fail) {
            sdTestPassed = false;
            refreshStatus();
            headline.setText("FAIL: microSD test: " + fail.getMessage());
        }
    }

    private void requestOverlay() {
        if (Settings.canDrawOverlays(this)) {
            overlayDetails.setText("Overlay permission already granted. "
                + "Still verify orb positioning with an emulator.");
            return;
        }
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (ActivityNotFoundException | SecurityException error) {
            overlayDetails.setText("Android did not expose the overlay shortcut. "
                + "Open Android Settings → Apps → Special app access → Display over other apps.");
        }
    }

    private String manual(CheckBox checkbox) {
        return checkbox.isChecked() ? "USER REPORTED PASS" : "NOT TESTED";
    }

    private String report() {
        android.util.DisplayMetrics display = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(display);
        String sdStatus;
        try {
            Uri folder = selectedFolder();
            sdStatus = "READ PASS; " + new MemoryStore(this, folder).load().size()
                + " memories, " + new GameProfileStore(this, folder).load().size()
                + " custom games; write test: " + (sdTestPassed ? "PASS" : "NOT TESTED");
        } catch (Exception missing) {
            sdStatus = "NEEDS ACTION (no private folder information included)";
        }
        return "Gaming AI v" + appVersion() + " | First-day device QA\n"
            + "Model: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
            + "Android: " + Build.VERSION.RELEASE + "\n"
            + "Display: " + display.widthPixels + "x" + display.heightPixels
            + " px; " + TouchUi.widthDp(this) + " dp; density "
            + String.format(Locale.US, "%.2f", display.density) + "\n"
            + "microSD: " + sdStatus + "\n"
            + "Overlay permission: " + (Settings.canDrawOverlays(this) ? "GRANTED" : "OFF")
            + " (game overlay test separate)\n"
            + "Touch: " + manual(touchPassed) + "\n"
            + "Swivel: " + manual(swivelPassed) + "\n"
            + "Offline voice: " + manual(audioPassed) + "\n"
            + "PPSSPP + orb: " + manual(ppssppPassed) + "\n"
            + "No screenshot, game data, note content or folder URI included.";
    }

    private void copyReport() {
        ClipboardManager clipboard =
            (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            headline.setText("Clipboard unavailable. Please report results manually.");
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText("Gaming AI QA", report()));
        headline.setText("Copied privacy-safe device report. "
            + "Review it before sharing; other apps may access your clipboard.");
    }

    @Override protected void onResume() {
        super.onResume();
        if (screenDetails != null) refreshStatus();
    }
}
