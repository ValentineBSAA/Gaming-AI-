package com.valentinebsaa.gamingai;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure-Java deduplication and bounded storage; can be unit-tested without Android. */
public final class MemoryPolicy {
    public static final int MAX_ENTRIES = 500;
    public static final int MAX_BYTES = 256 * 1024;
    private MemoryPolicy() {}

    public static List<MemoryRecord> compact(List<MemoryRecord> input, int maxEntries, int maxBytes) {
        if (maxEntries < 1 || maxBytes < 1) throw new IllegalArgumentException("Invalid memory budget");
        List<MemoryRecord> newestFirst = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = input.size() - 1; i >= 0; i--) {
            MemoryRecord item = input.get(i);
            String key = item.game.trim().toLowerCase(Locale.ROOT) + "\n"
                       + item.note.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (item.pinned || seen.add(key)) newestFirst.add(item);
        }
        Collections.reverse(newestFirst);
        while (newestFirst.size() > maxEntries || approxBytes(newestFirst) > maxBytes) {
            int victim = -1;
            for (int i = 0; i < newestFirst.size(); i++) {
                if (!newestFirst.get(i).pinned) { victim = i; break; }
            }
            if (victim == -1) throw new IllegalStateException("Pinned memories exceed budget; nothing was deleted.");
            newestFirst.remove(victim);
        }
        return newestFirst;
    }

    public static int approxBytes(List<MemoryRecord> records) {
        int bytes = 60; // JSON envelope
        for (MemoryRecord entry : records) {
            bytes += 150 + entry.id.getBytes(StandardCharsets.UTF_8).length
                         + entry.game.getBytes(StandardCharsets.UTF_8).length
                         + entry.note.getBytes(StandardCharsets.UTF_8).length;
        }
        return bytes;
    }
}
