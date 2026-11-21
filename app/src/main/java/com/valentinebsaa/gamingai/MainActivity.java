package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.os.Build;
import android.provider.Settings;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Offline v0.1 memory-shell UI. Does not pretend to be a live AI voice service. */
public final class MainActivity extends Activity {
    private static final int PICK_SD_FOLDER = 10;
    private static final String PREFS = "permission_locator_only";
    private static final String KEY_URI = "sd_tree_uri";
    private TextView status;
    private TextView overview;
    private TextView deviceInfo;
    private ScrollView mainScroll;
    private TextView recent;
    private Spinner game;
    private EditText note;
    private CheckBox pinned;
    private CheckBox autoColor;
    private TextView colorHex;
    private final SeekBar[] rgbBars = new SeekBar[3];
    private final ImageView[] orbPreview = new ImageView[3];
    private final int background = Color.rgb(15, 21, 35);
    private final int foreground = Color.rgb(242, 236, 244);
    private final int muted = Color.rgb(185, 176, 194);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        mainScroll = TouchUi.page(this);
        LinearLayout root = TouchUi.root(this, mainScroll);
        setContentView(mainScroll);
        mainScroll.requestFocus();

        TouchUi.header(this, root, "Gaming AI", "ROTATE • OFFLINE COMPANION");
        addText(root, "v0.1.9 • Offline companion", 12, muted);
        android.widget.GridLayout quick = TouchUi.quickGrid(this, root);

        final LinearLayout voiceCard = TouchUi.card(this, root);
        final LinearLayout storageCard = TouchUi.card(this, root);
        final LinearLayout orbCard = TouchUi.card(this, root);
        final LinearLayout memoryCard = TouchUi.card(this, root);
        final LinearLayout deviceCard = TouchUi.card(this, root);

        TouchUi.quickTile(this, quick, "Voice Lab", "Hear offline voices",
            TouchUi.TEAL, v -> openVoiceLab());
        TouchUi.quickTile(this, quick, "Memories", "Review & back up",
            TouchUi.LAVENDER, v ->
                startActivity(new Intent(this, MemoryLibraryActivity.class)));
        TouchUi.quickTile(this, quick, "Game Profiles", "Add your games",
            TouchUi.LAVENDER, v ->
                startActivity(new Intent(this, GameProfilesActivity.class)));
        TouchUi.quickTile(this, quick, "First-day Setup", "Check your Rotate",
            TouchUi.TEAL, v ->
                startActivity(new Intent(this, SetupGuideActivity.class)));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        TouchUi.rail(this, root).addView(shortcuts);
        TouchUi.chip(this, shortcuts, "Orb & colors",
            v -> TouchUi.jumpTo(this, mainScroll, orbCard));
        TouchUi.chip(this, shortcuts, "microSD",
            v -> TouchUi.jumpTo(this, mainScroll, storageCard));
        TouchUi.chip(this, shortcuts, "Device",
            v -> TouchUi.jumpTo(this, mainScroll, deviceCard));

        addText(voiceCard, "OFFLINE VOICE LAB", 15, foreground);
        button(voiceCard, "Open Voice Lab & offline voice guide", v -> openVoiceLab());
        addText(voiceCard, "Voice Lab only speaks when asked. No Piper or paid subscription. "
            + "Available voices depend on your Android installation.", 12, muted);

        addText(storageCard, "MICROSD MEMORY", 15, foreground);
        overview = addText(storageCard, "Choose a folder on your removable microSD.", 13, muted);
        button(storageCard, "Choose microSD subfolder", this::chooseFolder);
        button(storageCard, "Test SD write / read / delete", this::testSd);

        addText(orbCard, "FLOATING COMPANION", 15, foreground);
        button(orbCard, "Show draggable orb over games", this::enableOverlay);
        button(orbCard, "Hide companion orb", this::hideOverlay);
        setupOrbColors(orbCard);
        addText(orbCard, "Optional overlay. Drag to reposition and tap to return here. "
            + "The indicator reports the app's last state, not game awareness.", 12, muted);

        addText(memoryCard, "SAVE A MEMORY", 15, foreground);
        game = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, GameProfilePolicy.defaults());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        game.setAdapter(adapter);
        game.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(219, 159, 196)));
        game.setMinimumHeight(dp(54));
        memoryCard.addView(game, new LinearLayout.LayoutParams(-1, dp(58)));

        note = new EditText(this);
        note.setHint("What should the companion remember?");
        note.setHintTextColor(muted);
        note.setTextColor(foreground);
        note.setMinLines(2);
        note.setMaxLines(4);
        note.setTextSize(15);
        note.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
            android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        memoryCard.addView(note);

        pinned = new CheckBox(this);
        pinned.setText("Protect this memory from compaction");
        pinned.setTextColor(foreground);
        pinned.setMinHeight(dp(48));
        memoryCard.addView(pinned);

        button(memoryCard, "Save memory to SD", this::saveNote);
        button(memoryCard, "Offline push-to-talk · Open Voice Lab", v -> openVoiceLab());
        button(memoryCard, "Review / archive & compact notes", this::compact);
        button(memoryCard, "Refresh saved memories", this::refresh);
        button(memoryCard, "Open Memory Library · edit, pin, backup", v ->
            startActivity(new Intent(this, MemoryLibraryActivity.class)));
        button(memoryCard, "Manage Game Profiles · add your own games", v ->
            startActivity(new Intent(this, GameProfilesActivity.class)));
        status = addText(memoryCard, "No recording occurs on this screen. "
            + "Voice Lab's microphone activates only when requested.", 13, muted);
        addText(memoryCard, "RECENT MEMORIES", 15, foreground);
        recent = addText(memoryCard, "Nothing loaded yet.", 13, foreground);

        addText(deviceCard, "DEVICE READINESS", 15, foreground);
        deviceInfo = addText(deviceCard, "", 13, muted);
        button(deviceCard, "Refresh screen and permission report", v -> refreshDiagnostics());
        button(deviceCard, "Guided setup & test checklist",
            v -> startActivity(new Intent(this, SetupGuideActivity.class)));
        CheckBox reduceMotion = new CheckBox(this);
        reduceMotion.setText("Reduce interface animations");
        reduceMotion.setTextColor(foreground);
        reduceMotion.setMinHeight(dp(48));
        reduceMotion.setChecked(getSharedPreferences(TouchUi.PREFS, MODE_PRIVATE)
            .getBoolean(TouchUi.REDUCED_MOTION, false));
        deviceCard.addView(reduceMotion);
        reduceMotion.setOnCheckedChangeListener((view, value) -> {
            getSharedPreferences(TouchUi.PREFS, MODE_PRIVATE).edit()
                .putBoolean(TouchUi.REDUCED_MOTION, value).apply();
            if (value) {
                root.animate().cancel();
                root.setAlpha(1f);
                root.setTranslationY(0f);
            }
        });
        addText(deviceCard, "Privacy: no network permission, screen capture, account or API keys. "
            + "Selected microSD folder URI and interface choices are kept in Android settings.",
            12, muted);
        refreshDiagnostics();
        reloadGameSelector();
        refresh();
        TouchUi.reveal(this, root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView addText(LinearLayout parent, String content, int size, int color) {
        TextView text = new TextView(this);
        text.setText(content);
        text.setTextSize(size);
        text.setTextColor(color);
        TouchUi.decorateLabel(text, content, size);
        text.setPadding(0, dp(9), 0, dp(9));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }

    private void button(LinearLayout parent, String label, View.OnClickListener action) {
        TouchUi.button(this, parent, label, action);
    }

    private Uri selectedUri() throws IOException {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_URI, null);
        if (raw == null) throw new IOException("Choose a removable microSD subfolder first.");
        Uri uri = Uri.parse(raw);
        if (!StorageRules.isRemovableSdSubfolder(uri.getAuthority(),
                DocumentsContract.getTreeDocumentId(uri))) {
            throw new IOException("Saved folder is not a verified removable microSD subfolder.");
        }
        return uri;
    }

    private MemoryStore store() throws IOException { return new MemoryStore(this, selectedUri()); }

    private void enableOverlay(View v) {
        if (!Settings.canDrawOverlays(this)) {
            Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            try {
                startActivity(permission);
                setStatus("Allow Display over other apps in Android settings, then return and tap Show again.");
            } catch (Exception e) {
                setStatus("Overlay permission screen unavailable: " + e.getMessage());
            }
            return;
        }
        try {
            Intent service = new Intent(this, FloatingOrbService.class);
            service.setAction(FloatingOrbService.ACTION_SHOW);
            service.putExtra(FloatingOrbService.EXTRA_STATUS, "IDLE");
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("overlay_enabled", true).apply();
            setStatus("Overlay enabled. Drag the orb anywhere; tap it to open Gaming AI.");
        } catch (Exception e) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean("overlay_enabled", false).apply();
            setStatus("Unable to start overlay: " + e.getMessage());
        }
    }

    private void hideOverlay(View v) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean("overlay_enabled", false).apply();
        stopService(new Intent(this, FloatingOrbService.class));
        setStatus("Overlay hidden.");
    }

    private void setupOrbColors(LinearLayout root) {
        addText(root, "COLOR YOUR COMPANION", 15, foreground);
        FrameLayout preview = new FrameLayout(this);
        LinearLayout.LayoutParams frame = new LinearLayout.LayoutParams(dp(88), dp(88));
        frame.bottomMargin = dp(5);
        root.addView(preview, frame);
        int[] images = {R.drawable.orb_core, R.drawable.orb_petals, R.drawable.orb_ring};
        for (int i = 0; i < images.length; i++) {
            ImageView layer = new ImageView(this);
            layer.setImageResource(images[i]);
            layer.setScaleType(ImageView.ScaleType.FIT_CENTER);
            preview.addView(layer, new FrameLayout.LayoutParams(-1, -1));
            orbPreview[i] = layer;
        }
        colorHex = addText(root, "", 13, foreground);
        int initial = getSharedPreferences(OrbPalette.PREFS, MODE_PRIVATE)
            .getInt(OrbPalette.COLOR_KEY, OrbPalette.DEFAULT_COLOR);

        autoColor = new CheckBox(this);
        autoColor.setText("Automatic status colors");
        autoColor.setTextColor(foreground);
        autoColor.setChecked(getSharedPreferences(OrbPalette.PREFS, MODE_PRIVATE)
            .getBoolean(OrbPalette.AUTO_KEY, false));
        root.addView(autoColor);
        addText(root, "Automatic uses real app status today; AI-chosen colors will come later.",
            12, muted);

        String[] names = {"Red", "Green", "Blue"};
        int[] levels = {(initial >> 16) & 255, (initial >> 8) & 255, initial & 255};
        for (int i = 0; i < 3; i++) {
            addText(root, names[i], 12, muted);
            SeekBar bar = new SeekBar(this);
            bar.setMax(255);
            bar.setProgress(levels[i]);
            rgbBars[i] = bar;
            root.addView(bar, new LinearLayout.LayoutParams(-1, dp(36)));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seek, int value, boolean user) {
                    renderColorPreview();
                }
                @Override public void onStartTrackingTouch(SeekBar seek) {}
                @Override public void onStopTrackingTouch(SeekBar seek) {
                    if (autoColor.isChecked()) autoColor.setChecked(false);
                    saveColorAndRefreshOrb();
                }
            });
        }

        LinearLayout choices = new LinearLayout(this);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        TouchUi.rail(this, root).addView(choices);
        int[] presets = {0xffb997e8, 0xffff91bd, 0xff71dece, 0xffffffff};
        String[] labels = {"Violet", "Rose", "Teal", "White"};
        for (int i = 0; i < presets.length; i++) {
            int selected = presets[i];
            Button preset = new Button(this);
            preset.setText(labels[i]);
            preset.setAllCaps(false);
            TouchUi.styleButton(this, preset);
            preset.setTextSize(12);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(
                dp(SquareLayoutPolicy.presetWidthDp(TouchUi.widthDp(this))), dp(56));
            size.rightMargin = dp(6);
            choices.addView(preset, size);
            preset.setOnClickListener(v -> {
                autoColor.setChecked(false);
                rgbBars[0].setProgress((selected >> 16) & 255);
                rgbBars[1].setProgress((selected >> 8) & 255);
                rgbBars[2].setProgress(selected & 255);
                saveColorAndRefreshOrb();
            });
        }
        autoColor.setOnCheckedChangeListener((control, enabled) -> {
            for (SeekBar bar : rgbBars) bar.setEnabled(!enabled);
            saveColorAndRefreshOrb();
        });
        for (SeekBar bar : rgbBars) bar.setEnabled(!autoColor.isChecked());
        renderColorPreview();
    }

    private int customOrbColor() {
        return 0xff000000 | (rgbBars[0].getProgress() << 16)
            | (rgbBars[1].getProgress() << 8) | rgbBars[2].getProgress();
    }

    private void renderColorPreview() {
        if (rgbBars[2] == null || colorHex == null) return;
        int chosen = customOrbColor();
        boolean automatic = autoColor != null && autoColor.isChecked();
        int tint = OrbPalette.resolve(automatic, chosen, "IDLE");
        for (ImageView layer : orbPreview) {
            if (layer != null) layer.setColorFilter(tint, PorterDuff.Mode.MULTIPLY);
        }
        colorHex.setText(String.format(java.util.Locale.US, "#%06X  •  %s",
            chosen & 0x00ffffff, automatic ? "Automatic (idle preview)" : "Custom"));
    }

    private void saveColorAndRefreshOrb() {
        if (rgbBars[2] == null) return;
        getSharedPreferences(OrbPalette.PREFS, MODE_PRIVATE).edit()
            .putInt(OrbPalette.COLOR_KEY, customOrbColor())
            .putBoolean(OrbPalette.AUTO_KEY, autoColor.isChecked()).apply();
        renderColorPreview();
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("overlay_enabled", false)
                && Settings.canDrawOverlays(this)) {
            Intent theme = new Intent(this, FloatingOrbService.class);
            theme.setAction(FloatingOrbService.ACTION_THEME);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(theme);
                else startService(theme);
            } catch (Exception ignored) {
                // On background-service restriction, the next manual Show refreshes colors.
            }
        }
    }

    private void chooseFolder(View v) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, PICK_SD_FOLDER); }
        catch (ActivityNotFoundException e) { setStatus("Android folder picker is unavailable."); }
    }

    private void openVoiceLab() {
        Intent launch = new Intent(this, VoiceLabActivity.class);
        launch.putExtra(VoiceLabActivity.EXTRA_GAME,
            game != null && game.getSelectedItem() != null
                ? String.valueOf(game.getSelectedItem()) : "General");
        startActivity(launch);
    }

    /** Rebuild the game selector from the microSD catalog and existing memory titles. */
    private void reloadGameSelector() {
        if (game == null) return;
        String previous = game.getSelectedItem() == null ? "General"
            : String.valueOf(game.getSelectedItem());
        List<String> available = GameProfilePolicy.defaults();
        try {
            String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_URI, null);
            if (raw != null) {
                Uri uri = Uri.parse(raw);
                List<String> custom = new GameProfileStore(this, uri).load();
                List<MemoryRecord> notes = new MemoryStore(this, uri).load();
                available = GameProfilePolicy.allGames(custom, notes);
            }
        } catch (Exception unavailable) {
            // An inaccessible microSD must not prevent the main UI from opening.
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, available);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        game.setAdapter(adapter);
        int selected = available.indexOf(previous);
        game.setSelection(selected < 0 ? 0 : selected);
    }

    private void refreshDiagnostics() {
        if (deviceInfo == null) return;
        android.util.DisplayMetrics display = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(display);
        int dpWidth = TouchUi.widthDp(this);
        String selectedFolder = getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(KEY_URI, null);
        deviceInfo.setText("Reported display: " + display.widthPixels + " × "
            + display.heightPixels + " physical pixels"
            + "\nUsable width: " + dpWidth + " dp"
            + "  •  density: "
            + String.format(java.util.Locale.US, "%.2f", display.density)
            + "\nAndroid " + Build.VERSION.RELEASE
            + "  •  " + Build.MANUFACTURER + " " + Build.MODEL
            + "\nOverlay permission: "
            + (Settings.canDrawOverlays(this) ? "Granted" : "Off")
            + "\nMicroSD folder: " + (selectedFolder == null
                ? "Not selected" : "Selected; rerun write/read/delete test")
            + "\n720 × 720 target: touch scrolling responds to user display scaling."
            + "\nReal-device layouts, keyboard and swivel not yet verified.");
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (result != RESULT_OK || data == null) return;
        if (req == PICK_SD_FOLDER && data.getData() != null) {
            Uri uri = data.getData();
            try {
                if (!StorageRules.isRemovableSdSubfolder(uri.getAuthority(),
                        DocumentsContract.getTreeDocumentId(uri))) {
                    throw new IOException("Choose a subfolder on the removable microSD, not internal storage/card root.");
                }
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if (flags != (Intent.FLAG_GRANT_READ_URI_PERMISSION |
                              Intent.FLAG_GRANT_WRITE_URI_PERMISSION)) {
                    throw new IOException("Read/write access was not granted.");
                }
                getContentResolver().takePersistableUriPermission(uri, flags);
                new MemoryStore(this, uri).selfTest();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_URI, uri.toString()).apply();
                setStatus("Verified removable SD folder and temporary file cleanup.");
                reloadGameSelector();
                refresh();
            } catch (Exception e) { setStatus("Folder rejected: " + e.getMessage()); }
        }
    }

    private void testSd(View v) {
        try { store().selfTest(); setStatus("PASS: SD test written, read, and deleted."); }
        catch (Exception e) { setStatus("FAIL: " + e.getMessage()); }
    }

    private void saveNote(View v) {
        String value = note.getText().toString().trim();
        if (value.isEmpty() || value.length() > 500) {
            setStatus("Memory note must contain between 1 and 500 characters.");
            return;
        }
        try {
            MemoryStore storage = store();
            List<MemoryRecord> records = storage.load();
            records.add(new MemoryRecord(UUID.randomUUID().toString(),
                    System.currentTimeMillis(), String.valueOf(game.getSelectedItem()),
                    value, pinned.isChecked()));
            storage.save(records);
            note.setText("");
            pinned.setChecked(false);
            setStatus("Saved to removable microSD. Snapshot read-back verified.");
            refresh();
        } catch (Exception e) { setStatus("NOT SAVED: " + e.getMessage()); }
    }

    private void compact(View v) {
        try {
            MemoryStore storage = store();
            List<MemoryRecord> current = storage.load();
            List<MemoryRecord> compacted = MemoryPolicy.compact(current,
                MemoryPolicy.MAX_ENTRIES, MemoryPolicy.MAX_BYTES);
            int removed = current.size() - compacted.size();
            if (removed == 0) {
                setStatus("Already compact. All " + current.size() + " memories are preserved.");
                return;
            }
            String initialDigest = CombinedBackupRules.sha256(
                current, new java.util.ArrayList<>());
            new android.app.AlertDialog.Builder(this)
                .setTitle("Archive before removing old notes?")
                .setMessage("This will remove " + removed + " duplicate or older unpinned "
                    + "memories from the active library. Protected memories remain. "
                    + "First, Gaming AI creates and verifies a complete note backup "
                    + "on your microSD. Copy that backup off the card to keep a "
                    + "permanent history. Nothing is removed until you confirm.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Back up and compact", (dialog, button) -> {
                    try {
                        MemoryStore liveStore = store();
                        List<MemoryRecord> live = liveStore.load();
                        if (!initialDigest.equals(CombinedBackupRules.sha256(
                                live, new java.util.ArrayList<>()))) {
                            setStatus("Notes changed during review. Reopen compaction to check "
                                + "the latest memory list.");
                            return;
                        }
                        String recovery = liveStore.createBackup();
                        List<MemoryRecord> trimmed = MemoryPolicy.compact(live,
                            MemoryPolicy.MAX_ENTRIES, MemoryPolicy.MAX_BYTES);
                        liveStore.save(trimmed);
                        setStatus("Removed " + (live.size() - trimmed.size())
                            + " unpinned notes after a verified recovery backup: "
                            + recovery + ". Copy it off the card for safekeeping.");
                        refresh();
                    } catch (Exception failed) {
                        setStatus("No successful compaction: " + failed.getMessage()
                            + ". Check the active notes and recovery backup before retrying.");
                    }
                }).show();
        } catch (Exception e) {
            setStatus("Compaction preview unavailable: " + e.getMessage());
        }
    }

    @Override protected void onResume() {
        super.onResume();
        if (overview != null && recent != null) {
            reloadGameSelector();
            refresh();
        }
    }

    private void refresh(View ignored) { refresh(); }

    private void refresh() {
        try {
            MemoryStore storage = store();
            List<MemoryRecord> records = storage.load();
            overview.setText("microSD selected  •  " + records.size() + " memories"
                + "  •  budget 256 KiB / 500 notes");
            if (records.isEmpty()) { recent.setText("No saved memories yet."); return; }
            StringBuilder shown = new StringBuilder();
            int start = Math.max(0, records.size() - 8);
            for (int i = records.size() - 1; i >= start; i--) {
                MemoryRecord item = records.get(i);
                shown.append(item.pinned ? "★ " : "• ");
                shown.append(item.game).append("  •  ");
                shown.append(DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT).format(new Date(item.when)));
                shown.append("\n").append(item.note).append("\n\n");
            }
            recent.setText(shown.toString());
        } catch (Exception e) {
            overview.setText("No verified microSD folder available.");
            recent.setText("Stored memories are not loaded.");
        }
    }

    private void setStatus(String value) {
        if (status != null) status.setText(value);
        if (!getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean("overlay_enabled", false)
                || !Settings.canDrawOverlays(this)) return;
        Intent service = new Intent(this, FloatingOrbService.class);
        service.setAction(FloatingOrbService.ACTION_UPDATE);
        service.putExtra(FloatingOrbService.EXTRA_STATUS, OrbStatus.fromMessage(value));
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
            else startService(service);
        } catch (Exception e) {
            // A background-service restriction must not interrupt local SD operations.
        }
    }
}
