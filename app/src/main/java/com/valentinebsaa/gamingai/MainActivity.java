package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/** Offline v0.1 memory-shell UI. Does not pretend to be a live AI voice service. */
public final class MainActivity extends Activity {
    private static final int PICK_SD_FOLDER = 10;
    private static final int DICTATE_NOTE = 11;
    private static final String PREFS = "permission_locator_only";
    private static final String KEY_URI = "sd_tree_uri";
    private static final String[] GAMES = {
        "General", "Midnight Club 3: DUB Edition", "Midnight Club: LA Remix", "The 3rd Birthday"
    };

    private TextView status;
    private TextView overview;
    private TextView recent;
    private Spinner game;
    private EditText note;
    private CheckBox pinned;
    private final int background = Color.rgb(15, 21, 35);
    private final int foreground = Color.rgb(242, 236, 244);
    private final int muted = Color.rgb(185, 176, 194);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(background);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);
        setContentView(scroll);

        addText(root, "Gaming AI · Rotate", 26, foreground);
        addText(root, "Prototype 0.1.0  •  Offline memory shell", 13, muted);
        addText(root, "One companion, many games. Natural AI voice comes in Gate 2.", 15, foreground);
        overview = addText(root, "Choose a folder on your removable microSD.", 13, muted);
        button(root, "Choose microSD subfolder", this::chooseFolder);
        button(root, "Test SD write / read / delete", this::testSd);

        addText(root, "SAVE A MEMORY", 15, foreground);
        game = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, GAMES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        game.setAdapter(adapter);
        game.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(219, 159, 196)));
        root.addView(game);

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
        root.addView(note);

        pinned = new CheckBox(this);
        pinned.setText("Protect this memory from compaction");
        pinned.setTextColor(foreground);
        root.addView(pinned);

        button(root, "Save memory to SD", this::saveNote);
        button(root, "Dictate note (Android speech input)", this::dictate);
        button(root, "Compact unpinned memories", this::compact);
        button(root, "Refresh saved memories", this::refresh);
        status = addText(root, "No microphone or screen capture runs in this prototype.", 13, muted);
        addText(root, "RECENT MEMORIES", 15, foreground);
        recent = addText(root, "Nothing loaded yet.", 13, foreground);
        addText(root, "Privacy: no network permission, screenshots, account, or API keys. " +
            "Only the selected folder URI is kept internally by Android.", 12, muted);
        refresh();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView addText(LinearLayout parent, String content, int size, int color) {
        TextView text = new TextView(this);
        text.setText(content);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setPadding(0, dp(9), 0, dp(9));
        parent.addView(text, new LinearLayout.LayoutParams(-1, -2));
        return text;
    }

    private void button(LinearLayout parent, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTextColor(foreground);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(82, 44, 79)));
        button.setOnClickListener(action);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = dp(5);
        parent.addView(button, p);
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

    private void chooseFolder(View v) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { startActivityForResult(intent, PICK_SD_FOLDER); }
        catch (ActivityNotFoundException e) { setStatus("Android folder picker is unavailable."); }
    }

    private void dictate(View v) {
        Intent voice = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voice.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voice.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate a note, then save it to SD.");
        try { startActivityForResult(voice, DICTATE_NOTE); }
        catch (ActivityNotFoundException e) {
            setStatus("No Android speech recognizer installed. Manual notes still work.");
        }
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
                refresh();
            } catch (Exception e) { setStatus("Folder rejected: " + e.getMessage()); }
        } else if (req == DICTATE_NOTE) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                note.setText(matches.get(0));
                setStatus("Dictated text received. Review and tap Save memory to SD.");
            }
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
                    System.currentTimeMillis(), GAMES[game.getSelectedItemPosition()],
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
            List<MemoryRecord> before = storage.load();
            List<MemoryRecord> after = MemoryPolicy.compact(before,
                    MemoryPolicy.MAX_ENTRIES, MemoryPolicy.MAX_BYTES);
            storage.save(after);
            setStatus("Compacted " + before.size() + " to " + after.size()
                    + " records; protected entries retained.");
            refresh();
        } catch (Exception e) { setStatus("Compaction not applied: " + e.getMessage()); }
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

    private void setStatus(String value) { if (status != null) status.setText(value); }
}
