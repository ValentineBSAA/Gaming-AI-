package com.valentinebsaa.gamingai;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manual per-game organization. Does not search installed ROMs, scan PPSSPP,
 * infer currently running games, or create notes without user confirmation.
 */
public final class GameProfilesActivity extends HandheldActivity {
    private static final String PREFS = "permission_locator_only";
    private static final String URI_KEY = "sd_tree_uri";
    private static final int WHITE = 0xfff6f2ff;
    private static final int SECONDARY = 0xffb3bed1;
    private List<String> custom = new ArrayList<>();
    private List<MemoryRecord> notes = new ArrayList<>();
    private TextView status, selectedInfo;
    private EditText name;
    private ListView browser;
    private String selectedName;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = TouchUi.page(this);
        LinearLayout root = TouchUi.root(this, scroll);
        setContentView(scroll);
        TouchUi.header(this, root, "Game Profiles", "ONE COMPANION • MANY GAMES");
        TextView intro = new TextView(this);
        intro.setText("Organize your companion by game. Titles live on your microSD, "
            + "and existing memory entries remain untouched.");
        intro.setTextColor(SECONDARY);
        intro.setTextSize(13);
        root.addView(intro);

        LinearLayout createCard = TouchUi.card(this, root);
        heading(createCard, "ADD A GAME");
        name = new EditText(this);
        name.setHint("e.g. God of War: Chains of Olympus");
        name.setHintTextColor(SECONDARY);
        name.setTextColor(WHITE);
        name.setSingleLine(true);
        name.setTextSize(14);
        name.setFilters(new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(GameProfilePolicy.MAX_NAME)
        });
        name.setMinHeight(TouchUi.dp(this, 52));
        createCard.addView(name);
        TouchUi.button(this, createCard, "Add to microSD game library", view -> addGame());
        status = new TextView(this);
        status.setTextColor(SECONDARY);
        status.setTextSize(12);
        status.setPadding(0, TouchUi.dp(this, 8), 0, TouchUi.dp(this, 8));
        createCard.addView(status);

        LinearLayout library = TouchUi.card(this, root);
        heading(library, "YOUR CUSTOM PROFILES");
        browser = new ListView(this);
        browser.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        browser.setDividerHeight(TouchUi.dp(this, 2));
        browser.setVerticalScrollBarEnabled(true);
        browser.setOverScrollMode(android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        library.addView(browser, new LinearLayout.LayoutParams(
            -1, TouchUi.dp(this, SquareLayoutPolicy.voiceListHeightDp(
                TouchUi.widthDp(this)) + 30)));
        browser.setOnItemClickListener((parent, row, position, id) -> {
            if (position >= 0 && position < custom.size()) {
                selectedName = custom.get(position);
                browser.setItemChecked(position, true);
                selectedInfo();
            }
        });
        selectedInfo = new TextView(this);
        selectedInfo.setTextSize(13);
        selectedInfo.setTextColor(SECONDARY);
        selectedInfo.setPadding(0, TouchUi.dp(this, 9), 0, TouchUi.dp(this, 9));
        library.addView(selectedInfo);
        TouchUi.button(this, library, "Remove selected empty profile", view -> removeGame());
        TouchUi.button(this, library, "Refresh profiles", view -> reload());
        TextView warning = new TextView(this);
        warning.setText("Starter games cannot be removed. A profile containing saved notes "
            + "cannot be removed either. Removing an empty profile never deletes notes. "
            + "Game recognition is a future feature, not active in this version.");
        warning.setTextSize(12);
        warning.setTextColor(SECONDARY);
        root.addView(warning);
        TouchUi.button(this, root, "Back to Gaming AI", view -> finish());
        reload();
        TouchUi.reveal(this, root);
    }

    private void heading(LinearLayout parent, String caption) {
        TextView label = new TextView(this);
        label.setText(caption);
        label.setTextColor(WHITE);
        label.setTextSize(15);
        TouchUi.decorateLabel(label, caption, 15);
        parent.addView(label);
    }

    private Uri selectedUri() throws IOException {
        String value = getSharedPreferences(PREFS, MODE_PRIVATE).getString(URI_KEY, null);
        if (value == null) throw new IOException("Select a removable microSD folder in the dashboard first.");
        Uri uri = Uri.parse(value);
        if (!StorageRules.isRemovableSdSubfolder(uri.getAuthority(),
                DocumentsContract.getTreeDocumentId(uri))) {
            throw new IOException("Only a verified removable microSD subfolder is supported.");
        }
        return uri;
    }

    private void message(String text) {
        if (status != null) status.setText(text);
    }

    private void selectedInfo() {
        if (selectedName == null || !custom.contains(selectedName)) {
            selectedName = null;
            selectedInfo.setText("Select a custom profile to inspect it.");
            return;
        }
        int count = 0;
        for (MemoryRecord record : notes) if (selectedName.equalsIgnoreCase(record.game)) count++;
        selectedInfo.setText(selectedName + "\n" + count
            + " saved memories • Profile name stored on microSD");
    }

    private void render() {
        ArrayList<String> labels = new ArrayList<>();
        for (String title : custom) {
            int count = 0;
            for (MemoryRecord note : notes) if (title.equalsIgnoreCase(note.game)) count++;
            labels.add(title + "  •  " + count + " memories");
        }
        if (labels.isEmpty()) labels.add("No custom profiles yet");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
            android.R.layout.simple_list_item_1, labels) {
            @Override public android.view.View getView(int position,
                    android.view.View reused, android.view.ViewGroup parent) {
                TextView item = (TextView) super.getView(position, reused, parent);
                item.setTextColor(WHITE);
                item.setSingleLine(false);
                item.setMaxLines(3);
                item.setTextSize(13);
                item.setMinHeight(TouchUi.dp(GameProfilesActivity.this, 60));
                item.setPadding(12, 9, 12, 9);
                item.setBackgroundColor(position < custom.size()
                    && custom.get(position).equals(selectedName)
                        ? 0xff3b4b67 : 0xff253147);
                return item;
            }
        };
        browser.setAdapter(adapter);
        int index = custom.indexOf(selectedName);
        if (index >= 0) browser.setItemChecked(index, true);
        else browser.clearChoices();
        selectedInfo();
    }

    private void reload() {
        try {
            Uri uri = selectedUri();
            custom = new GameProfileStore(this, uri).load();
            notes = new MemoryStore(this, uri).load();
            render();
            message(custom.size() + " custom profiles, "
                + notes.size() + " existing notes. No automatic game detection.");
        } catch (Exception error) {
            custom = new ArrayList<>();
            notes = new ArrayList<>();
            selectedName = null;
            render();
            message("Can't load microSD profiles: " + error.getMessage());
        }
    }

    private void addGame() {
        try {
            Uri uri = selectedUri();
            List<String> current = new GameProfileStore(this, uri).load();
            List<MemoryRecord> existing = new MemoryStore(this, uri).load();
            String normalized = GameProfilePolicy.normalize(name.getText().toString());
            // A historical note title is already a discoverable game; don't duplicate it.
            for (String game : GameProfilePolicy.allGames(current, existing)) {
                if (normalized.equalsIgnoreCase(game))
                    throw new IllegalArgumentException("This game is already available.");
            }
            List<String> updated = GameProfilePolicy.add(current, normalized);
            new GameProfileStore(this, uri).save(updated);
            selectedName = normalized;
            name.setText("");
            reload();
            message("Created " + normalized + " on microSD. Add a note from the dashboard.");
        } catch (Exception error) {
            message("Profile not added: " + error.getMessage());
        }
    }

    private void removeGame() {
        if (selectedName == null) {
            message("Select an empty custom profile first.");
            return;
        }
        String removing = selectedName;
        int count = 0;
        for (MemoryRecord item : notes)
            if (removing.equalsIgnoreCase(item.game)) count++;
        if (count > 0) {
            message("This game has " + count + " notes. Keep the profile to preserve them.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle("Remove this empty profile?")
            .setMessage(removing + " has no saved notes. Removing this name only "
                + "updates the Gaming AI microSD profile index.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove", (dialog, which) -> {
                try {
                    Uri uri = selectedUri();
                    List<MemoryRecord> live = new MemoryStore(this, uri).load();
                    for (MemoryRecord item : live) {
                        if (removing.equalsIgnoreCase(item.game))
                            throw new IOException("Game now has notes; removal canceled.");
                    }
                    GameProfileStore store = new GameProfileStore(this, uri);
                    store.save(GameProfilePolicy.remove(store.load(), removing));
                    selectedName = null;
                    reload();
                    message("Removed empty profile. No memories were deleted.");
                } catch (Exception error) {
                    message("No change saved: " + error.getMessage());
                }
            }).show();
    }
}
