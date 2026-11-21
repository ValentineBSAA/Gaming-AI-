package com.valentinebsaa.gamingai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Pure deterministic game-title rules. No game observation or ROM scanning. */
public final class GameProfilePolicy {
    public static final int MAX_CUSTOM = 80;
    public static final int MAX_NAME = 64;
    private static final String[] DEFAULT_GAMES = {
        "General",
        "Midnight Club 3: DUB Edition",
        "Midnight Club: LA Remix",
        "The 3rd Birthday"
    };
    private GameProfilePolicy() {}

    public static List<String> defaults() {
        ArrayList<String> result = new ArrayList<>();
        for (String game : DEFAULT_GAMES) result.add(game);
        return result;
    }

    public static String normalize(String name) {
        if (name == null) throw new IllegalArgumentException("Enter a game title.");
        String result = name.trim().replaceAll("\\s+", " ");
        if (result.length() < 1 || result.length() > MAX_NAME
                || "All games".equalsIgnoreCase(result)
                || result.matches(".*[\\p{Cntrl}].*")) {
            throw new IllegalArgumentException("Use a game name of 1–64 characters.");
        }
        return result;
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> validateCustom(List<String> names) {
        if (names == null || names.size() > MAX_CUSTOM)
            throw new IllegalArgumentException("At most 80 custom game profiles are allowed.");
        LinkedHashMap<String, String> found = new LinkedHashMap<>();
        for (String name : defaults()) found.put(key(name), name);
        ArrayList<String> custom = new ArrayList<>();
        for (String input : names) {
            String name = normalize(input);
            String canonical = key(name);
            if (found.containsKey(canonical))
                throw new IllegalArgumentException("Duplicate or starter game: " + name);
            found.put(canonical, name);
            custom.add(name);
        }
        return custom;
    }

    public static List<String> add(List<String> existing, String requested) {
        ArrayList<String> next = new ArrayList<>(validateCustom(existing));
        next.add(normalize(requested));
        return validateCustom(next);
    }

    public static List<String> remove(List<String> existing, String title) {
        List<String> current = validateCustom(existing);
        ArrayList<String> next = new ArrayList<>(current);
        boolean found = false;
        for (String name : current) {
            if (name.equalsIgnoreCase(title == null ? "" : title.trim())) {
                next.remove(name);
                found = true;
                break;
            }
        }
        if (!found) throw new IllegalArgumentException(
            "Only removable custom profiles can be deleted.");
        return next;
    }

    /** Always preserve titles from existing notes, even if the profile index is absent. */
    public static List<String> allGames(List<String> custom, List<MemoryRecord> notes) {
        LinkedHashMap<String, String> games = new LinkedHashMap<>();
        for (String title : defaults()) games.put(key(title), title);
        for (String title : validateCustom(custom)) games.put(key(title), title);
        if (notes != null) {
            for (MemoryRecord note : notes) {
                if (note.game == null || note.game.trim().isEmpty()) continue;
                String title = note.game.trim();
                games.putIfAbsent(key(title), title);
            }
        }
        return new ArrayList<>(games.values());
    }
}
