package com.valentinebsaa.gamingai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Pure data transformations. Never mutates a caller's original memory list. */
public final class MemoryActions {
    private MemoryActions() {}

    public static List<MemoryRecord> filter(List<MemoryRecord> records,
            String game, String query) {
        String selectedGame = game == null ? "All games" : game;
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        ArrayList<MemoryRecord> found = new ArrayList<>();
        if (records == null) return found;
        for (MemoryRecord item : records) {
            if (!"All games".equals(selectedGame) && !selectedGame.equals(item.game)) continue;
            if (!needle.isEmpty() && !(item.note.toLowerCase(Locale.ROOT).contains(needle)
                    || item.game.toLowerCase(Locale.ROOT).contains(needle))) continue;
            found.add(item);
        }
        Collections.reverse(found); // newest first, preserving saved sequence
        return found;
    }

    public static List<MemoryRecord> replaceNote(List<MemoryRecord> existing,
            String id, String newText) {
        if (newText == null || newText.trim().isEmpty() || newText.trim().length() > 500) {
            throw new IllegalArgumentException("A memory note must be 1–500 characters.");
        }
        String normalized = newText.trim();
        ArrayList<MemoryRecord> result = new ArrayList<>();
        boolean found = false;
        for (MemoryRecord item : existing) {
            if (item.id.equals(id)) {
                result.add(new MemoryRecord(item.id, item.when, item.game,
                    normalized, item.pinned));
                found = true;
            } else result.add(item);
        }
        if (!found) throw new IllegalArgumentException("Memory no longer exists.");
        return result;
    }

    public static List<MemoryRecord> setPinned(List<MemoryRecord> existing,
            String id, boolean pinned) {
        ArrayList<MemoryRecord> result = new ArrayList<>();
        boolean found = false;
        for (MemoryRecord item : existing) {
            if (item.id.equals(id)) {
                result.add(new MemoryRecord(item.id, item.when, item.game,
                    item.note, pinned));
                found = true;
            } else result.add(item);
        }
        if (!found) throw new IllegalArgumentException("Memory no longer exists.");
        return result;
    }

    public static List<MemoryRecord> delete(List<MemoryRecord> existing, String id) {
        ArrayList<MemoryRecord> result = new ArrayList<>();
        boolean found = false;
        for (MemoryRecord item : existing) {
            if (item.id.equals(id)) found = true;
            else result.add(item);
        }
        if (!found) throw new IllegalArgumentException("Memory no longer exists.");
        return result;
    }
}
