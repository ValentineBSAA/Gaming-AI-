package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** User-controlled inspection, corrections and explicit backups on removable microSD. */
public final class MemoryLibraryActivity extends HandheldActivity {
    private static final String PREFS = "permission_locator_only";
    private static final String URI_KEY = "sd_tree_uri";
    private static final int PICK_BACKUP = 34;
    private static final int PICK_COMBINED_BACKUP = 35;
    private static final int TEXT = Color.rgb(242, 236, 244);
    private static final int MUTED = Color.rgb(185, 176, 194);
    private TextView status;
    private TextView count;
    private TextView details;
    private EditText search;
    private Spinner gameFilter;
    private ListView list;
    private ArrayAdapter<String> adapter;
    private List<MemoryRecord> originals = new ArrayList<>();
    private List<MemoryRecord> visible = new ArrayList<>();
    private String selectedId;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = TouchUi.page(this);
        LinearLayout root = TouchUi.root(this, scroll);
        setContentView(scroll);
        scroll.requestFocus();
        TouchUi.header(this, root, "Memory Library", "PRIVATE NOTES • MICROSD");
        addText(root, "Your games and memories, carefully organized.", 13, MUTED);
        status = addText(root, "Checking your saved memories...", 12, MUTED);

        LinearLayout browseCard = TouchUi.card(this, root);
        addText(browseCard, "FIND A MEMORY", 15, TEXT);
        count = addText(browseCard, "Loading...", 13, MUTED);
        search = new EditText(this);
        search.setSingleLine(true);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setTextSize(14);
        search.setHint("Search notes or game titles");
        search.setMinHeight(dp(52));
        browseCard.addView(search);
        gameFilter = new Spinner(this);
        ArrayList<String> initialFilters = new ArrayList<>();
        initialFilters.add("All games");
        initialFilters.addAll(GameProfilePolicy.defaults());
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, initialFilters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameFilter.setAdapter(filterAdapter);
        browseCard.addView(gameFilter, new LinearLayout.LayoutParams(-1, dp(56)));
        list = new ListView(this);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setVerticalScrollBarEnabled(true);
        list.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        list.setDividerHeight(dp(2));
        browseCard.addView(list, new LinearLayout.LayoutParams(-1,
            dp(SquareLayoutPolicy.voiceListHeightDp(TouchUi.widthDp(this)) + 35)));
        details = addText(browseCard, "Select a memory to inspect it.", 12, MUTED);
        list.setOnItemClickListener((parent, row, position, id) -> {
            if (position >= 0 && position < visible.size()) {
                selectedId = visible.get(position).id;
                list.setItemChecked(position, true);
                if (adapter != null) adapter.notifyDataSetChanged();
                describeSelection();
            }
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                filterList();
            }
            @Override public void afterTextChanged(Editable value) {}
        });
        gameFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                filterList();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout controls = TouchUi.card(this, root);
        addText(controls, "SELECTED MEMORY", 15, TEXT);
        TouchUi.button(this, controls, "Edit text", v -> editSelected());
        TouchUi.button(this, controls, "Pin / unpin", v -> togglePin());
        TouchUi.button(this, controls, "Delete with confirmation", v -> deleteSelected());
        TouchUi.button(this, controls, "Refresh memories", v -> reload());

        LinearLayout backup = TouchUi.card(this, root);
        addText(backup, "BACKUP AND RESTORE", 15, TEXT);
        addText(backup, "These backups contain memory notes, not the separate custom-game index. Plaintext JSON may include private information. "
            + "Keep them safe. A backup on this same microSD does not protect against card loss.",
            12, MUTED);
        TouchUi.button(this, backup, "Create COMPLETE backup · notes + games",
            v -> confirmCompleteBackup());
        TouchUi.button(this, backup, "Preview / restore COMPLETE backup",
            v -> chooseCompleteBackup());
        addText(backup, "Legacy note-only tools (older backups)", 12, MUTED);
        TouchUi.button(this, backup, "Back up memory notes on microSD", v -> confirmBackup());
        TouchUi.button(this, backup, "Preview and restore memory notes", v -> chooseBackup());
        addText(backup, "Restoring replaces current memories only after a separate confirmation. "
            + "Previewing a file never changes anything.", 12, MUTED);
        TouchUi.button(this, root, "Back to Gaming AI", v -> finish());
        reload();
        TouchUi.reveal(this, root);
    }

    private int dp(int value) { return TouchUi.dp(this, value); }

    private TextView addText(LinearLayout parent, String value, int size, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        TouchUi.decorateLabel(label, value, size);
        label.setPadding(0, dp(9), 0, dp(9));
        parent.addView(label, new LinearLayout.LayoutParams(-1, -2));
        return label;
    }

    private MemoryStore store() throws IOException {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(URI_KEY, null);
        if (raw == null) throw new IOException("First select a removable microSD folder on the main screen.");
        Uri uri = Uri.parse(raw);
        if (!StorageRules.isRemovableSdSubfolder(uri.getAuthority(),
                DocumentsContract.getTreeDocumentId(uri))) {
            throw new IOException("This is not an approved removable microSD subfolder.");
        }
        return new MemoryStore(this, uri);
    }

    private void show(String value) { if (status != null) status.setText(value); }

    private MemoryRecord selected() {
        if (selectedId == null) return null;
        for (MemoryRecord item : originals) {
            if (selectedId.equals(item.id)) return item;
        }
        return null;
    }

    private void describeSelection() {
        MemoryRecord item = selected();
        details.setText(item == null
            ? "Select a memory to inspect it."
            : (item.pinned ? "★ Protected" : "Unpinned") + "  •  " + item.game
                + "\n" + DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT).format(new Date(item.when))
                + "\n" + item.note);
    }

    private void filterList() {
        if (search == null || gameFilter == null || list == null) return;
        String filter = String.valueOf(gameFilter.getSelectedItem());
        visible = MemoryActions.filter(originals, filter, search.getText().toString());
        ArrayList<String> lines = new ArrayList<>();
        for (MemoryRecord record : visible) {
            String excerpt = record.note.replaceAll("[\\r\\n]+", " ");
            if (excerpt.length() > 95) excerpt = excerpt.substring(0, 95) + "…";
            lines.add((record.pinned ? "★ " : "● ") + record.game + "\n" + excerpt);
        }
        if (lines.isEmpty()) lines.add("No matching memories");
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, lines) {
            @Override public View getView(int position, View convert, android.view.ViewGroup parent) {
                TextView cell = (TextView) super.getView(position, convert, parent);
                cell.setSingleLine(false);
                cell.setMaxLines(3);
                cell.setTextColor(TEXT);
                cell.setTextSize(13);
                cell.setPadding(dp(12), dp(12), dp(12), dp(12));
                cell.setMinHeight(dp(65));
                cell.setBackgroundColor(position < visible.size()
                    && visible.get(position).id.equals(selectedId) ? 0xff3b4b67 : 0xff253147);
                return cell;
            }
        };
        list.setAdapter(adapter);
        int selectedIndex = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).id.equals(selectedId)) selectedIndex = i;
        }
        if (selectedIndex >= 0) list.setItemChecked(selectedIndex, true);
        else {
            list.clearChoices();
            selectedId = null;
        }
        count.setText(visible.size() + " matching • " + originals.size() + " total");
        describeSelection();
    }

    private void reload() {
        try {
            originals = store().load();
            refreshGameFilters();
            filterList();
            show("Loaded " + originals.size() + " verified memories from microSD.");
        } catch (Exception problem) {
            originals = new ArrayList<>();
            selectedId = null;
            filterList();
            show("Cannot read microSD memories: " + problem.getMessage());
        }
    }

    private void refreshGameFilters() {
        String old = gameFilter.getSelectedItem() == null ? "All games"
            : String.valueOf(gameFilter.getSelectedItem());
        ArrayList<String> filters = new ArrayList<>();
        filters.add("All games");
        try {
            String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(URI_KEY, null);
            if (raw != null) {
                List<String> custom = new GameProfileStore(this, Uri.parse(raw)).load();
                filters.addAll(GameProfilePolicy.allGames(custom, originals));
            } else {
                filters.addAll(GameProfilePolicy.allGames(
                    new ArrayList<>(), originals));
            }
        } catch (Exception unavailable) {
            filters.addAll(GameProfilePolicy.allGames(new ArrayList<>(), originals));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameFilter.setAdapter(adapter);
        int oldIndex = filters.indexOf(old);
        gameFilter.setSelection(oldIndex < 0 ? 0 : oldIndex);
    }

    private void apply(List<MemoryRecord> updated, String success) {
        try {
            store().save(updated);
            reload();
            show(success);
        } catch (Exception error) {
            show("No change saved: " + error.getMessage());
        }
    }

    private void editSelected() {
        MemoryRecord item = selected();
        if (item == null) { show("Select a memory first."); return; }
        EditText editor = new EditText(this);
        editor.setText(item.note);
        editor.setTextColor(TEXT);
        editor.setSingleLine(false);
        editor.setMaxLines(6);
        editor.setPadding(dp(16), dp(10), dp(16), dp(10));
        editor.setFilters(new android.text.InputFilter[] {
            new android.text.InputFilter.LengthFilter(500)
        });
        new AlertDialog.Builder(this)
            .setTitle("Edit saved memory")
            .setView(editor)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", (dialog, button) -> {
                try {
                    apply(MemoryActions.replaceNote(originals, item.id,
                        editor.getText().toString()), "Memory updated on microSD.");
                } catch (Exception error) {
                    show("Unable to edit memory: " + error.getMessage());
                }
            }).show();
    }

    private void togglePin() {
        MemoryRecord item = selected();
        if (item == null) { show("Select a memory first."); return; }
        apply(MemoryActions.setPinned(originals, item.id, !item.pinned),
            item.pinned ? "Memory unpinned." : "Memory protected from automatic compaction.");
    }

    private void deleteSelected() {
        MemoryRecord item = selected();
        if (item == null) { show("Select a memory first."); return; }
        new AlertDialog.Builder(this)
            .setTitle(item.pinned ? "Delete protected memory?" : "Delete this memory?")
            .setMessage("This removes the selected note from the current "
                + "memory library. Your most recent snapshot may still hold an older copy. "
                + "Create an external backup first if needed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (dialog, button) -> {
                apply(MemoryActions.delete(originals, item.id), "Selected memory removed.");
            }).show();
    }

    private CombinedBackupStore combined() throws IOException {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(URI_KEY, null);
        if (raw == null) throw new IOException("Choose your microSD folder in the dashboard first.");
        return new CombinedBackupStore(this, Uri.parse(raw));
    }

    private void confirmCompleteBackup() {
        new AlertDialog.Builder(this)
            .setTitle("Export your complete Gaming AI library?")
            .setMessage("This makes one plaintext JSON file containing BOTH your saved "
                + "memories and custom game titles. It is not encrypted. The file will "
                + "stay on the selected microSD, so also copy it to another device "
                + "to protect against card loss.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create and verify", (dialog, action) -> {
                try {
                    String filename = combined().create();
                    show("Complete backup verified: " + filename
                        + ". Keep an additional copy off the microSD.");
                } catch (Exception error) {
                    show("No complete backup created: " + error.getMessage());
                }
            }).show();
    }

    private void chooseCompleteBackup() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/json");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(picker, PICK_COMBINED_BACKUP); }
        catch (Exception error) {
            show("Android file picker unavailable: " + error.getMessage());
        }
    }

    private void previewCompleteBackup(Uri selected) {
        final CombinedBackupStore.Preview preview;
        try { preview = combined().inspect(selected); }
        catch (Exception error) {
            show("Backup rejected: " + error.getMessage());
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Verified COMPLETE Gaming AI backup")
            .setMessage(preview.memories.size() + " memories (" + preview.pinnedCount()
                + " protected) plus " + preview.customGames.size()
                + " custom games.\nSHA-256: " + preview.checksum.substring(0, 16)
                + "…\n\nThis is only a preview. No data has been changed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue to confirmation",
                (dialog, action) -> confirmCompleteRestore(preview))
            .show();
    }

    private void confirmCompleteRestore(CombinedBackupStore.Preview preview) {
        new AlertDialog.Builder(this)
            .setTitle("Replace notes AND game profiles?")
            .setMessage("The app will first export an emergency copy of your current "
                + "memories and custom game titles to microSD, then restore both "
                + "sections and verify them. If writing fails, it attempts rollback. "
                + "For true backup protection, keep a copy off the card.")
            .setNegativeButton("Keep current data", null)
            .setPositiveButton("Restore both", (dialog, action) -> {
                try {
                    String detail = combined().restore(preview);
                    reload();
                    show(detail);
                } catch (Exception error) {
                    reload();
                    show("RESTORE NOT VERIFIED: " + error.getMessage());
                }
            }).show();
    }

    private void confirmBackup() {
        new AlertDialog.Builder(this)
            .setTitle("Create plaintext backup?")
            .setMessage("Your notes can contain private information. The JSON backup stays "
                + "on the selected microSD. A second copy on a separate device is recommended.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create", (dialog, button) -> {
                try {
                    String file = store().createBackup();
                    show("Verified backup created: " + file
                        + ". Copy it off the card for real disaster recovery.");
                } catch (Exception error) {
                    show("Backup failed: " + error.getMessage());
                }
            }).show();
    }

    private void chooseBackup() {
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("application/json");
        picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try { startActivityForResult(picker, PICK_BACKUP); }
        catch (Exception error) { show("Android file picker unavailable: " + error.getMessage()); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request == PICK_COMBINED_BACKUP) {
            previewCompleteBackup(data.getData());
            return;
        }
        if (request != PICK_BACKUP) return;
        final List<MemoryRecord> importRecords;
        try { importRecords = store().inspectBackup(data.getData()); }
        catch (Exception error) {
            show("Backup preview failed: " + error.getMessage());
            return;
        }
        int pins = 0;
        for (MemoryRecord item : importRecords) if (item.pinned) pins++;
        new AlertDialog.Builder(this)
            .setTitle("Preview Gaming AI backup")
            .setMessage(importRecords.size() + " memories (" + pins + " protected). "
                + "Restoring will replace current notes after a second confirmation. "
                + "No files are changed by this preview.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Continue", (dialog, button) -> confirmRestore(importRecords))
            .show();
    }

    private void confirmRestore(List<MemoryRecord> replacement) {
        new AlertDialog.Builder(this)
            .setTitle("Replace existing memories?")
            .setMessage("This will replace the current Gaming AI memory library on microSD. "
                + "Your previous snapshot remains as the alternate recovery slot, "
                + "but create a separate backup before proceeding.")
            .setNegativeButton("Keep current", null)
            .setPositiveButton("Restore backup", (dialog, button) -> {
                apply(replacement, "Backup restored and verified on removable microSD.");
            }).show();
    }
}
